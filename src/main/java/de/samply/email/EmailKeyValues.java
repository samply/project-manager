package de.samply.email;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.Query;
import de.samply.document.DocumentService;
import de.samply.frontend.FrontendService;
import de.samply.project.ProjectBridgeheadService;
import de.samply.user.UserService;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.ProjectUtils;
import de.samply.utils.UserUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EmailKeyValues {

    @Getter
    private final Map<String, String> keyValues = new HashMap<>();
    private final FrontendService frontendService;
    private final DocumentService documentService;
    private final UserService userService;
    private final ProjectBridgeheadService projectBridgeheadService;

    private final BridgeheadsConfiguration bridgeheadsConfiguration;


    public EmailKeyValues(FrontendService frontendService,
                          EmailContext emailContext,
                          DocumentService documentService,
                          UserService userService,
                          BridgeheadsConfiguration bridgeheadsConfiguration,
                          String researchEnvironmentUrl,
                          ProjectBridgeheadService projectBridgeheadService
    ) {
        this.frontendService = frontendService;
        this.documentService = documentService;
        this.userService = userService;
        this.bridgeheadsConfiguration = bridgeheadsConfiguration;
        this.projectBridgeheadService = projectBridgeheadService;
        keyValues.putAll(emailContext.getContext());
        addKeyValue(EmailContextKey.RESEARCH_ENVIRONMENT_URL, researchEnvironmentUrl);
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
        emailRecipient.getBridgehead()
                .<Runnable>map(pb -> () -> add(pb))
                .or(() -> emailRecipient.getProject().map(p -> () -> add(p)))
                .orElse(() -> {
                    emailRecipient.getProject().ifPresent(this::addProject);
                    emailRecipient.getBridgehead().ifPresent(this::addBridgehead);
                })
                .run();
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("UnusedReturnValue")
    public EmailKeyValues addMessage(String message) {
        addKeyValue(EmailContextKey.MESSAGE, message);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public EmailKeyValues addProject(Project project) {
        addKeyValue(EmailContextKey.PROJECT_CODE, project.getCode());
        addKeyValue(EmailContextKey.PROJECT_VIEW_URL,
                this.frontendService.fetchUrl(ProjectManagerConst.PROJECT_VIEW_SITE,
                        Map.of(ProjectManagerConst.PROJECT_CODE, project.getCode())));
        return this;
    }

    public EmailKeyValues add(ProjectBridgehead projectBridgehead) {
        if (projectBridgehead != null) {
            addBridgehead(projectBridgehead);
            add(projectBridgehead.getProject());
            addKeyValue(EmailContextKey.PROJECT_BRIDGEHEAD_RESULTS_URL, projectBridgehead.getResultsUrl());
            addBridgeheadAdmin(projectBridgehead);
        }
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public EmailKeyValues addBridgeheadAdmin(ProjectBridgehead bridgehead) {
        if (bridgehead != null) {
            userService
                    .fetchFirstBridgeheadAdmin(bridgehead)
                    .ifPresent(bridgeheadAdminUser ->
                            addEmailData(
                                    bridgeheadAdminUser.getEmail(),
                                    EmailContextKey.BRIDGEHEAD_ADMIN_EMAIL,
                                    EmailContextKey.BRIDGEHEAD_ADMIN_FIRST_NAME,
                                    EmailContextKey.BRIDGEHEAD_ADMIN_LAST_NAME,
                                    EmailContextKey.BRIDGEHEAD_ADMIN_NAME));
        }
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public EmailKeyValues addBridgehead(ProjectBridgehead bridgehead) {
        addKeyValue(EmailContextKey.BRIDGEHEAD, fetchHumanReadableBridgehead(bridgehead));
        addBridgeheadAdmin(bridgehead);
        return this;
    }

    private String fetchHumanReadableBridgehead(ProjectBridgehead bridgehead) {
        Optional<String> humanReadable = bridgeheadsConfiguration.getHumanReadable(bridgehead.getBridgehead());
        return humanReadable.orElse(bridgehead.getBridgehead());
    }

    public EmailKeyValues add(Project project) {
        if (project != null) {
            addProject(project);
            addEmailData(project.getCreatorEmail(),
                    EmailContextKey.PROJECT_CREATOR_EMAIL,
                    EmailContextKey.PROJECT_CREATOR_FIRST_NAME,
                    EmailContextKey.PROJECT_CREATOR_LAST_NAME,
                    EmailContextKey.PROJECT_CREATOR_NAME);
            addKeyValue(EmailContextKey.QUERY,
                    (project.getQuery().getHumanReadable()) != null ?
                            project.getQuery().getHumanReadable() : project.getQuery().getQuery());
            addKeyValue(EmailContextKey.PROJECT_TYPE, () -> ProjectUtils.formatWithCommasAndAnd(project.fetchProjectTypes()));
            addKeyValue(EmailContextKey.PROJECT_RESULTS_URL, project.getResultsUrl());
            add(project.getQuery());
            addBridgeheads(project);
            addLastDocument(project);
        }
        return this;
    }

    private void addBridgeheads(Project project) {
        addKeyValue(EmailContextKey.PROJECT_BRIDGEHEADS,
                projectBridgeheadService
                        .fetchBridgeheads(project)
                        .stream()
                        .map(this::fetchHumanReadableBridgehead)
                        .collect(Collectors.joining(", ")));
    }

    private void addLastDocument(Project project) {
        documentService.fetchDocumentOrderByCreatedAtDesc(project).ifPresent(projectDocument -> {
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

    private void addEmailData(String email, @NotNull EmailContextKey emailKey, @NotNull EmailContextKey emailFirstNameKey, @NotNull EmailContextKey emailLastNameKey, @NotNull EmailContextKey emailNameKey) {
        if (email != null) {
            addKeyValue(emailKey, email);
            userService.fetchUser(email).ifPresent(user -> {
                addKeyValue(emailFirstNameKey, user::getFirstName);
                addKeyValue(emailLastNameKey, user::getLastName);
                addKeyValue(emailNameKey, () -> UserUtils.extractFullName(Optional.of(user)));
            });
        }
    }

    public String replaceHtmlVariables(String htmlText) {
        return replaceHtmlVariables(htmlText, keyValues);
    }

    public static String replaceHtmlVariables(String htmlText, Map<String, String> keyValues) {
        if (htmlText != null) {
            // Regular expression to match the variable pattern
            // e.g. <variable1/> It is like an HTML tag
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

                // Replace the variable with its value or keep it as-is if not found
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
