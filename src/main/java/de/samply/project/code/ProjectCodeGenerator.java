package de.samply.project.code;

import de.samply.app.ProjectManagerConst;
import de.samply.db.repository.ProjectRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Generates project codes from the {@code PROJECT_ID_TEMPLATE} environment variable.
 *
 * <p>Templates consist of literal text and tokens enclosed in double braces:
 * {@code {{YEAR}}}, {@code {{MONTH}}}, {@code {{DAY}}}, {@code {{HOUR}}},
 * {@code {{MINUTE}}}, {@code {{SECOND}}}, {@code {{SEQUENCE:n}}},
 * {@code {{NUMBER:n}}}, and {@code {{STRING:n}}}.
 * {@code SEQUENCE} takes the next value of the database sequence
 * {@code samply.project_code_seq}, zero-padded to at least {@code n} digits
 * (it keeps growing beyond {@code n} digits instead of wrapping around).
 * {@code NUMBER} produces {@code n} random decimal digits and {@code STRING}
 * {@code n} random lowercase hexadecimal characters.</p>
 *
 * <p>Every template must contain at least one {@code SEQUENCE}, {@code NUMBER} or
 * {@code STRING} token, so that projects created at the same time get different
 * codes. A generated code that already exists is discarded and generated again.
 * If {@link #MAX_ATTEMPTS} attempts all hit existing codes (a very narrow random
 * token, or old codes from a previous template), the last attempt gets a suffix
 * {@code -n} with the next value of {@code samply.project_code_seq}. Sequence
 * values never repeat, so this always ends with an unused code. The unique
 * constraint on {@code project.code} is the final guard.</p>
 *
 * <p>Literal text is deliberately restricted to ASCII letters, digits, hyphens,
 * and underscores. Project codes are used as URL path segments, so this prevents
 * spaces, separators, query markers, fragments, percent escapes, and other
 * characters from changing URL parsing or requiring URL encoding. Invalid
 * templates are rejected when this component is created.</p>
 */
@Component
public class ProjectCodeGenerator {

    static final String DEFAULT_TEMPLATE = "{{STRING:20}}";
    static final int MAX_ATTEMPTS = 10;
    private static final int MAX_TOKEN_LENGTH = 64;
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([A-Za-z]+)(?::([0-9]+))?}}");
    private static final Pattern SAFE_LITERAL = Pattern.compile("[A-Za-z0-9_-]*");
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    /** A parsed part of the template: either literal text or a token with its length. */
    private record Segment(String literal, ProjectCodeToken token, int length) {
    }

    private final String template;
    private final List<Segment> segments;
    private final ProjectRepository projectRepository;
    private final Clock clock;
    private final SecureRandom random;

    /**
     * Creates the generator using the configured project ID template.
     *
     * @param template configured template; blank values use the legacy random
     *                 20-character hexadecimal format
     * @throws IllegalArgumentException if the template contains an unknown,
     *                                  malformed, or unsafe token or literal, or
     *                                  no token that distinguishes projects
     */
    @Autowired
    public ProjectCodeGenerator(@Value(ProjectManagerConst.PROJECT_ID_TEMPLATE_SV) String template,
                                ProjectRepository projectRepository) {
        this(template, projectRepository, Clock.systemDefaultZone(), new SecureRandom());
    }

    ProjectCodeGenerator(String template, ProjectRepository projectRepository, Clock clock, SecureRandom random) {
        this.template = template == null || template.isBlank() ? DEFAULT_TEMPLATE : template;
        this.segments = parse(this.template);
        this.projectRepository = projectRepository;
        this.clock = clock;
        this.random = random;
    }

    /**
     * Generates a project code from the configured template that no existing project uses.
     *
     * @return a project code containing the configured literal and expanded token values,
     *         followed by {@code -n} if {@link #MAX_ATTEMPTS} attempts only produced existing codes
     */
    public String generate() {
        String code = render();
        for (int attempt = 1; attempt < MAX_ATTEMPTS && projectRepository.existsByCode(code); attempt++) {
            code = render();
        }
        // Sequence values never repeat, so each candidate is new and the loop ends
        String baseCode = code;
        while (projectRepository.existsByCode(code)) {
            code = baseCode + "-" + projectRepository.nextProjectCodeSequenceValue();
        }
        return code;
    }

    private String render() {
        LocalDateTime now = LocalDateTime.now(clock);
        StringBuilder result = new StringBuilder();
        for (Segment segment : segments) {
            result.append(segment.token() == null ? segment.literal() : resolve(segment, now));
        }
        return result.toString();
    }

    private String resolve(Segment segment, LocalDateTime now) {
        return switch (segment.token()) {
            case YEAR -> String.format("%04d", now.getYear());
            case MONTH -> String.format("%02d", now.getMonthValue());
            case DAY -> String.format("%02d", now.getDayOfMonth());
            case HOUR -> String.format("%02d", now.getHour());
            case MINUTE -> String.format("%02d", now.getMinute());
            case SECOND -> String.format("%02d", now.getSecond());
            case SEQUENCE -> String.format("%0" + segment.length() + "d", projectRepository.nextProjectCodeSequenceValue());
            case NUMBER -> randomCharacters(segment.length(), 10);
            case STRING -> randomCharacters(segment.length(), 16);
        };
    }

    private String randomCharacters(int length, int radix) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(HEX_DIGITS[random.nextInt(radix)]);
        }
        return result.toString();
    }

    private static List<Segment> parse(String template) {
        List<Segment> segments = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(template);
        int end = 0;
        while (matcher.find()) {
            addLiteral(segments, template, template.substring(end, matcher.start()));
            segments.add(parseToken(template, matcher.group(1), matcher.group(2)));
            end = matcher.end();
        }
        addLiteral(segments, template, template.substring(end));
        if (segments.stream().noneMatch(segment -> segment.token() != null && segment.token().isDistinguishing())) {
            throw new IllegalArgumentException("Invalid PROJECT_ID_TEMPLATE '" + template
                    + "': it must contain at least one {{SEQUENCE:n}}, {{NUMBER:n}} or {{STRING:n}} token"
                    + " so that projects created at the same time get different codes.");
        }
        return List.copyOf(segments);
    }

    private static void addLiteral(List<Segment> segments, String template, String literal) {
        if (!SAFE_LITERAL.matcher(literal).matches()) {
            String invalidCharacters = literal.codePoints()
                    .filter(codePoint -> !SAFE_LITERAL.matcher(Character.toString(codePoint)).matches())
                    .distinct()
                    .mapToObj(codePoint -> "'" + Character.toString(codePoint) + "'")
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid PROJECT_ID_TEMPLATE '" + template
                    + "': literal text '" + literal + "' contains unsupported character(s) "
                    + invalidCharacters + ". Literal text may contain only ASCII letters, digits, '-' and '_'"
                    + " because project codes are used as URL path segments.");
        }
        if (!literal.isEmpty()) {
            segments.add(new Segment(literal, null, 0));
        }
    }

    private static Segment parseToken(String template, String key, String lengthText) {
        ProjectCodeToken token;
        try {
            token = ProjectCodeToken.valueOf(key);
        } catch (IllegalArgumentException e) {
            throw invalidToken(template, key, "unknown token");
        }
        if (!token.isLengthRequired()) {
            if (lengthText != null) {
                throw invalidToken(template, key + ":" + lengthText, "this token takes no length");
            }
            return new Segment(null, token, 0);
        }
        if (lengthText == null) {
            throw invalidToken(template, key, "a length is required, e.g. {{" + key + ":5}}");
        }
        int length = lengthText.length() > 3 ? Integer.MAX_VALUE : Integer.parseInt(lengthText);
        if (length < 1 || length > MAX_TOKEN_LENGTH) {
            throw invalidToken(template, key + ":" + lengthText,
                    "the length must be between 1 and " + MAX_TOKEN_LENGTH);
        }
        return new Segment(null, token, length);
    }

    private static IllegalArgumentException invalidToken(String template, String token, String reason) {
        return new IllegalArgumentException("Invalid token {{" + token + "}} in PROJECT_ID_TEMPLATE '"
                + template + "': " + reason + ".");
    }
}
