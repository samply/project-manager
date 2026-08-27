package de.samply.user;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.User;
import de.samply.db.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Seeds the existing global mailing blacklist from deployment configuration.
 * The import is additive: administrators may maintain additional blacklist
 * entries through the frontend, and those database entries are never removed.
 *
 * <p>Example file configured through {@code MAILING_BLACK_LIST_FILE_PATH}:</p>
 * <pre>{@code
 * # Managers who do not want project notifications
 * manager@example.org # optional explanation
 * another-manager@example.org
 * }</pre>
 */
@Slf4j
@Component
public class MailingBlacklistFileImporter implements ApplicationRunner {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    private final UserRepository userRepository;
    private final String blacklistFilePath;

    public MailingBlacklistFileImporter(
            UserRepository userRepository,
            @Value(ProjectManagerConst.MAILING_BLACK_LIST_FILE_PATH_SV) String blacklistFilePath) {
        this.userRepository = userRepository;
        this.blacklistFilePath = blacklistFilePath;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (!StringUtils.hasText(blacklistFilePath)) {
            log.info("Mailing blacklist file path not provided; no users imported");
            return;
        }

        // ApplicationRunner executes after Spring has initialized the database
        // repositories, so this import is safe to perform in one transaction.
        Path path = Path.of(blacklistFilePath.trim());
        ParsedEmails parsed = readEmails(path);
        // Grouping the per-user result keeps the startup log useful without
        // logging the configured email addresses themselves.
        Map<ImportOutcome, Long> outcomes = parsed
                .emails()
                .stream()
                .map(this::importEmail)
                .collect(Collectors.groupingBy(outcome -> outcome, Collectors.counting()));

        log.info("Imported {} mailing-blacklist entries from {} (created: {}, updated: {}, unchanged: {})",
                parsed.emails().size(), path,
                outcomes.getOrDefault(ImportOutcome.CREATED, 0L),
                outcomes.getOrDefault(ImportOutcome.UPDATED, 0L),
                outcomes.getOrDefault(ImportOutcome.UNCHANGED, 0L));
    }

    private ImportOutcome importEmail(String email) {
        Optional<User> existing = userRepository.findByEmail(email);
        // Capture this before changing the entity; the same managed object is
        // returned by the repository and would otherwise look newly enabled.
        boolean wasBlacklisted = existing.map(User::isInMailingBlackList).orElse(false);
        User user = existing.orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            return newUser;
        });
        if (existing.isEmpty() || !user.isInMailingBlackList()) {
            user.setInMailingBlackList(true);
            userRepository.save(user);
        }
        return existing.isEmpty()
                ? ImportOutcome.CREATED
                : wasBlacklisted ? ImportOutcome.UNCHANGED : ImportOutcome.UPDATED;
    }

    private ParsedEmails readEmails(Path path) {
        try {
            try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
                Map<Boolean, List<String>> entries = lines
                        // The first '#' starts a comment, including inline
                        // comments; emails themselves cannot contain '#'.
                        .map(line -> line.split("#", 2)[0].trim())
                        .filter(StringUtils::hasText)
                        .collect(Collectors.partitioningBy(EMAIL_PATTERN.asPredicate()));
                // Keep valid addresses in file order while removing duplicates;
                // invalid entries remain a list so duplicate errors are counted.
                Set<String> emails = new LinkedHashSet<>(entries.getOrDefault(true, List.of()));
                int invalid = entries.getOrDefault(false, List.of()).size();
                if (invalid > 0) {
                    log.warn("Ignored {} invalid email entries in mailing blacklist file {}", invalid, path);
                }
                return new ParsedEmails(emails);
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Failed to read mailing blacklist file " + path, exception);
        }
    }

    private record ParsedEmails(Set<String> emails) {
    }

    private enum ImportOutcome {CREATED, UPDATED, UNCHANGED}
}
