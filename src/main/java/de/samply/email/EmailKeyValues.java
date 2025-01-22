package de.samply.email;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.Query;
import de.samply.db.repository.*;
import de.samply.frontend.FrontendService;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EmailKeyValues {

    private Map<String, String> keyValues = new HashMap<>();
    private final FrontendService frontendService;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;


    public EmailKeyValues(FrontendService frontendService,
                          EmailContext emailContext,
                          ProjectBridgeheadRepository projectBridgeheadRepository,
                          ProjectRepository projectRepository,
                          UserRepository userRepository,
                          BridgeheadConfiguration bridgeheadConfiguration,
                          ProjectDocumentRepository projectDocumentRepository,
                          BridgeheadAdminUserRepository bridgeheadAdminUserRepository) {
        this.frontendService = frontendService;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.projectDocumentRepository = projectDocumentRepository;
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
        keyValues.putAll(emailContext.getContext());
    }

    public EmailKeyValues add(EmailRecipient emailRecipient) {
        if (emailRecipient != null) {
            addEmailData(emailRecipient.getEmail(),
                    EmailContextKey.EMAIL_TO,
                    EmailContextKey.EMAIL_TO_FIRST_NAME,
                    EmailContextKey.EMAIL_TO_LAST_NAME,
                    EmailContextKey.EMAIL_TO_NAME);
            emailRecipient.getMessage().ifPresent(this::addMessage);
            add(emailRecipient.getRole());
            addProjectBridgeheadOrProject(emailRecipient);
        }
        return this;
    }

    private void addProjectBridgeheadOrProject(EmailRecipient emailRecipient) {
        AtomicReference<Optional<ProjectBridgehead>> projectBridgeheadOptional = new AtomicReference<>(Optional.empty());
        AtomicReference<Optional<Project>> projectOptional = new AtomicReference<>(Optional.empty());
        emailRecipient.getProjectCode().ifPresent(projectCode ->
                projectRepository.findByCode(projectCode).ifPresent(project ->
                        emailRecipient.getBridgehead().ifPresentOrElse(bridgehead ->
                                        projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project).ifPresentOrElse(projectBridgehead ->
                                                        projectBridgeheadOptional.set(Optional.of(projectBridgehead)),
                                                () -> projectOptional.set(Optional.of(project))),
                                () -> projectOptional.set(Optional.of(project)))));
        if (projectBridgeheadOptional.get().isPresent()) {
            add(projectBridgeheadOptional.get().get());
        } else if (projectOptional.get().isPresent()) {
            add(projectOptional.get().get());
        } else {
            emailRecipient.getProjectCode().ifPresent(this::addProjectCode);
            emailRecipient.getBridgehead().ifPresent(this::addBridgehead);
        }
    }

    public EmailKeyValues add(ProjectBridgeheadUser projectBridgeheadUser) {
        if (projectBridgeheadUser != null) {
            addEmailData(projectBridgeheadUser.getEmail(),
                    EmailContextKey.PROJECT_BRIDGEHEAD_USER_EMAIL,
                    EmailContextKey.PROJECT_BRIDGEHEAD_USER_FIRST_NAME,
                    EmailContextKey.PROJECT_BRIDGEHEAD_USER_LAST_NAME,
                    EmailContextKey.PROJECT_BRIDGEHEAD_USER_NAME);
            add(projectBridgeheadUser.getProjectRole());
            add(projectBridgeheadUser.getProjectBridgehead());
        }
        return this;
    }

    public EmailKeyValues add(ProjectRole projectRole) {
        addKeyValue(EmailContextKey.PROJECT_ROLE, projectRole.toString());
        return this;
    }

    public EmailKeyValues addMessage(String message) {
        addKeyValue(EmailContextKey.MESSAGE, message);
        return this;
    }

    public EmailKeyValues addProjectCode(String projectCode) {
        addKeyValue(EmailContextKey.PROJECT_CODE, projectCode);
        addKeyValue(EmailContextKey.PROJECT_VIEW_URL,
                this.frontendService.fetchUrl(ProjectManagerConst.PROJECT_VIEW_SITE,
                        Map.of(ProjectManagerConst.PROJECT_CODE, projectCode)));
        return this;
    }

    public EmailKeyValues add(ProjectBridgehead projectBridgehead) {
        if (projectBridgehead != null) {
            addBridgehead(projectBridgehead.getBridgehead());
            add(projectBridgehead.getProject());
            addKeyValue(EmailContextKey.PROJECT_BRIDGEHEAD_RESULTS_URL, projectBridgehead.getResultsUrl());
            addBridgeheadAdmin(projectBridgehead.getBridgehead());
        }
        return this;
    }

    public EmailKeyValues addBridgeheadAdmin(String bridgehead) {
        if (bridgehead != null) {
            this.bridgeheadAdminUserRepository.findFirstByBridgehead(bridgehead).ifPresent(bridgeheadAdminUser ->
                    addEmailData(bridgeheadAdminUser.getEmail(),
                            EmailContextKey.BRIDGEHEAD_ADMIN_EMAIL,
                            EmailContextKey.BRIDGEHEAD_ADMIN_FIRST_NAME,
                            EmailContextKey.BRIDGEHEAD_ADMIN_LAST_NAME,
                            EmailContextKey.BRIDGEHEAD_ADMIN_NAME));
        }
        return this;
    }

    public EmailKeyValues addBridgehead(String bridgehead) {
        addKeyValue(EmailContextKey.BRIDGEHEAD, fetchHumanReadableBridgehead(bridgehead));
        addBridgeheadAdmin(bridgehead);
        return this;
    }

    private String fetchHumanReadableBridgehead(String bridgehead) {
        Optional<String> humanReadable = bridgeheadConfiguration.getHumanReadable(bridgehead);
        return humanReadable.isPresent() ? humanReadable.get() : bridgehead;
    }

    public EmailKeyValues add(Project project) {
        if (project != null) {
            addProjectCode(project.getCode());
            addEmailData(project.getCreatorEmail(),
                    EmailContextKey.PROJECT_CREATOR_EMAIL,
                    EmailContextKey.PROJECT_CREATOR_FIRST_NAME,
                    EmailContextKey.PROJECT_CREATOR_LAST_NAME,
                    EmailContextKey.PROJECT_CREATOR_NAME);
            addKeyValue(EmailContextKey.QUERY,
                    (project.getQuery().getHumanReadable()) != null ?
                            project.getQuery().getHumanReadable() : project.getQuery().getQuery());
            addKeyValue(EmailContextKey.PROJECT_TYPE, () -> project.getType().toString());
            addKeyValue(EmailContextKey.PROJECT_RESULTS_URL, project.getResultsUrl());
            add(project.getQuery());
            addBridgeheads(project);
            addLastDocument(project);
        }
        return this;
    }

    private void addBridgeheads(Project project) {
        addKeyValue(EmailContextKey.PROJECT_BRIDGEHEADS,
                projectBridgeheadRepository.findByProject(project).stream()
                        .map(projectBridgehead -> fetchHumanReadableBridgehead(projectBridgehead.getBridgehead()))
                        .collect(Collectors.joining(", ")));
    }

    private void addLastDocument(Project project) {
        projectDocumentRepository.findTopByProjectOrderByCreatedAtDesc(project).ifPresent(projectDocument -> {
            addKeyValue(EmailContextKey.LAST_DOCUMENT_LABEL, projectDocument::getLabel);
            addKeyValue(EmailContextKey.LAST_DOCUMENT_FILENAME, projectDocument::getOriginalFilename);
            addKeyValue(EmailContextKey.LAST_DOCUMENT_URL, projectDocument::getUrl);
            addEmailData(projectDocument.getCreatorEmail(),
                    EmailContextKey.LAST_DOCUMENT_SENDER_EMAIL,
                    EmailContextKey.LAST_DOCUMENT_SENDER_FIRST_NAME,
                    EmailContextKey.LAST_DOCUMENT_SENDER_LAST_NAME,
                    EmailContextKey.LAST_DOCUMENT_SENDER_NAME);
        });
    }

    public EmailKeyValues add(Query query) {
        if (query != null) {
            addKeyValue(EmailContextKey.QUERY,
                    (query.getHumanReadable()) != null ? query.getHumanReadable() : query.getQuery());
            addKeyValue(EmailContextKey.QUERY_LABEL, query::getLabel);
            addKeyValue(EmailContextKey.QUERY_DESCRIPTION, query::getDescription);
        }
        return this;
    }

    private void addKeyValue(@NotNull EmailContextKey key, Supplier<String> valueGetter) {
        addKeyValue(key.getValue(), valueGetter);
    }

    private void addKeyValue(@NotNull String key, Supplier<String> valueGetter) {
        String value = valueGetter.get();
        if (value != null) {
            keyValues.put(key, value);
        }
    }

    public void addKeyValue(@NotNull EmailContextKey key, @NotNull String value) {
        addKeyValue(key.getValue(), value);
    }

    public void addKeyValue(@NotNull String key, @NotNull String value) {
        keyValues.put(key, value);
    }

    public Map<String, String> getKeyValues() {
        return keyValues;
    }

    private void addEmailData(String email, @NotNull EmailContextKey emailKey, @NotNull EmailContextKey emailFirstNameKey, @NotNull EmailContextKey emailLastNameKey, @NotNull EmailContextKey emailNameKey) {
        if (email != null) {
            addKeyValue(emailKey, email);
            userRepository.findByEmail(email).ifPresent(user -> {
                addKeyValue(emailFirstNameKey, user::getFirstName);
                addKeyValue(emailLastNameKey, user::getLastName);
                addKeyValue(emailNameKey, () -> user.getFirstName() + " " + user.getLastName());
            });
        }
    }

    public String replaceHtmlVariables(String htmlText) {
        return replaceHtmlVariables(htmlText, keyValues);
    }

    public static String replaceHtmlVariables(String htmlText, Map<String, String> keyValues) {
        if (htmlText != null) {
            // Regular expression to match the variable pattern
            // e.g. <variable1/> It is like a HTML tag
            String regex = "<\\s*([a-zA-Z0-9_-]+)\\s*/>";

            // Use StringBuilder to build the result efficiently
            StringBuilder result = new StringBuilder();

            // Use Matcher to find all matches
            var matcher = Pattern.compile(regex).matcher(htmlText);
            int lastMatchEnd = 0; // Tracks the end of the last match

            while (matcher.find()) {
                // Append the part of the string before the match
                result.append(htmlText, lastMatchEnd, matcher.start());

                // Extract the variable name (group 1 in the regex)
                String variableName = matcher.group(1);

                // Replace the variable with its value, or keep it as-is if not found
                result.append(keyValues.getOrDefault(variableName, matcher.group()));

                // Update lastMatchEnd to the end of this match
                lastMatchEnd = matcher.end();
            }

            // Append the rest of the string after the last match
            result.append(htmlText, lastMatchEnd, htmlText.length());

            htmlText = result.toString();
        }
        return htmlText;
    }

}
