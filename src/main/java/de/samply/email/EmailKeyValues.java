package de.samply.email;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.Query;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.db.repository.UserRepository;
import de.samply.frontend.FrontendService;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class EmailKeyValues {

    private Map<String, String> keyValues = new HashMap<>();
    private final FrontendService frontendService;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;


    public EmailKeyValues(FrontendService frontendService,
                          EmailContext emailContext,
                          ProjectBridgeheadRepository projectBridgeheadRepository,
                          ProjectRepository projectRepository,
                          UserRepository userRepository) {
        this.frontendService = frontendService;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        keyValues.putAll(emailContext.getContext());
    }

    public EmailKeyValues add(EmailRecipient emailRecipient) {
        if (emailRecipient != null) {
            addEmailData(emailRecipient.getEmail(),
                    ProjectManagerConst.EMAIL_CONTEXT_EMAIL_TO,
                    ProjectManagerConst.EMAIL_CONTEXT_EMAIL_TO_FIRST_NAME,
                    ProjectManagerConst.EMAIL_CONTEXT_EMAIL_TO_LAST_NAME,
                    ProjectManagerConst.EMAIL_CONTEXT_EMAIL_TO_NAME);
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
                        emailRecipient.getBridgehead().ifPresent(bridgehead ->
                                projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project).ifPresentOrElse(projectBridgehead ->
                                                projectBridgeheadOptional.set(Optional.of(projectBridgehead)),
                                        () -> projectOptional.set(Optional.of(project))))));
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
                    ProjectManagerConst.EMAIL_CONTEXT_PROJECT_BRIDGEHEAD_USER_EMAIL,
                    ProjectManagerConst.EMAIL_CONTEXT_PROJECT_BRIDGEHEAD_USER_FIRST_NAME,
                    ProjectManagerConst.EMAIL_CONTEXT_PROJECT_BRIDGEHEAD_USER_LAST_NAME,
                    ProjectManagerConst.EMAIL_CONTEXT_PROJECT_BRIDGEHEAD_USER_NAME);
            add(projectBridgeheadUser.getProjectRole());
            add(projectBridgeheadUser.getProjectBridgehead());
        }
        return this;
    }

    public EmailKeyValues add(ProjectRole projectRole) {
        addKeyValue(ProjectManagerConst.EMAIL_CONTEXT_PROJECT_ROLE, projectRole.toString());
        return this;
    }

    public EmailKeyValues addMessage(String message) {
        addKeyValue(ProjectManagerConst.EMAIL_CONTEXT_MESSAGE, message);
        return this;
    }

    public EmailKeyValues addProjectCode(String projectCode) {
        addKeyValue(ProjectManagerConst.EMAIL_CONTEXT_PROJECT_CODE, projectCode);
        addKeyValue(ProjectManagerConst.EMAIL_CONTEXT_PROJECT_VIEW_URL,
                this.frontendService.fetchUrl(ProjectManagerConst.PROJECT_VIEW_SITE,
                        Map.of(ProjectManagerConst.PROJECT_CODE, projectCode)));
        return this;
    }

    public EmailKeyValues add(ProjectBridgehead projectBridgehead) {
        if (projectBridgehead != null) {
            addBridgehead(projectBridgehead.getBridgehead());
            add(projectBridgehead.getProject());
        }
        return this;
    }

    public EmailKeyValues addBridgehead(String bridgehead) {
        addKeyValue(ProjectManagerConst.EMAIL_CONTEXT_BRIDGEHEAD, bridgehead);
        return this;
    }


    public EmailKeyValues add(Project project) {
        if (project != null) {
            addProjectCode(project.getCode());
            addEmailData(project.getCreatorEmail(),
                    ProjectManagerConst.EMAIL_CONTEXT_PROJECT_CREATOR_EMAIL,
                    ProjectManagerConst.EMAIL_CONTEXT_PROJECT_CREATOR_FIRST_NAME,
                    ProjectManagerConst.EMAIL_CONTEXT_PROJECT_CREATOR_LAST_NAME,
                    ProjectManagerConst.EMAIL_CONTEXT_PROJECT_CREATOR_NAME);
            addKeyValue(ProjectManagerConst.EMAIL_CONTEXT_QUERY,
                    (project.getQuery().getHumanReadable()) != null ?
                            project.getQuery().getHumanReadable() : project.getQuery().getQuery());
            addKeyValue(ProjectManagerConst.EMAIL_CONTEXT_PROJECT_TYPE, () -> project.getType().toString());
            add(project.getQuery());
        }
        return this;
    }

    public EmailKeyValues add(Query query) {
        if (query != null) {
            addKeyValue(ProjectManagerConst.EMAIL_CONTEXT_QUERY,
                    (query.getHumanReadable()) != null ? query.getHumanReadable() : query.getQuery());
            addKeyValue(ProjectManagerConst.EMAIL_CONTEXT_QUERY_LABEL, query::getLabel);
            addKeyValue(ProjectManagerConst.EMAIL_CONTEXT_QUERY_DESCRIPTION, query::getDescription);
        }
        return this;
    }

    private void addKeyValue(@NotNull String key, Supplier<String> valueGetter) {
        String value = valueGetter.get();
        if (value != null) {
            keyValues.put(key, value);
        }
    }

    public void addKeyValue(@NotNull String key, @NotNull String value) {
        keyValues.put(key, value);
    }

    public Map<String, String> getKeyValues() {
        return keyValues;
    }

    private void addEmailData(String email, @NotNull String emailKey, @NotNull String emailFirstNameKey, @NotNull String emailLastNameKey, @NotNull String emailNameKey) {
        if (email != null) {
            addKeyValue(emailKey, email);
            userRepository.findByEmail(email).ifPresent(user -> {
                addKeyValue(emailFirstNameKey, user::getFirstName);
                addKeyValue(emailLastNameKey, user::getLastName);
                addKeyValue(emailNameKey, () -> user.getFirstName() + " " + user.getLastName());
            });
        }
    }

}
