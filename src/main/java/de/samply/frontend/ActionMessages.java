package de.samply.frontend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.security.SessionUser;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionMessages {

    @JsonProperty("messages")
    private Map<String, List<ActionMessage>> actionMessageMap;

    public Optional<List<ActionMessage>> getActionMessages(String action) {
        return Optional.ofNullable(actionMessageMap.get(action));
    }

    public Optional<ResolvedActionMessages> fetchMessages(
            @NotNull String action,
            @NotNull String module,
            @NotNull String language,
            Optional<Project> project,
            Optional<ProjectBridgehead> projectBridgehead,
            Optional<ProjectBridgeheadUser> projectBridgeheadUser,
            SessionUser sessionUser
    ) {
        for (ActionMessage message : getActionMessages(action).orElse(List.of())) {
            if (isRequiredMessage(module, project, projectBridgehead, projectBridgeheadUser, sessionUser, message)) {
                ResolvedActionMessages resolved = new ResolvedActionMessages(
                        localizedMessage(message.getExplanationMessages(), language),
                        localizedMessage(message.getSuccessMessages(), language),
                        localizedMessage(message.getErrorMessages(), language),
                        message.getPriority() != null ? message.getPriority() : 0
                );
                if (resolved.explanation() != null
                        || resolved.successMessage() != null
                        || resolved.errorMessage() != null) {
                    return Optional.of(resolved);
                }
            }
        }
        return Optional.empty();
    }

    private String localizedMessage(Map<String, String> messages, String language) {
        if (messages == null) {
            return null;
        }
        String message = messages.get(language);
        return message == null || message.isBlank() ? null : message;
    }

    private boolean isRequiredMessage(
            String module,
            Optional<Project> project,
            Optional<ProjectBridgehead> projectBridgehead,
            Optional<ProjectBridgeheadUser> projectBridgeheadUser,
            SessionUser sessionUser,
            ActionMessage message
    ) {
        if (module != null && message.getModule() != null && !module.equalsIgnoreCase(message.getModule())) {
            return false;
        }
        if (project.isPresent()) {
            if (message.getProjectType() != null && !project.get().hasProjectType(message.getProjectType())) {
                return false;
            }
            if (message.getProjectState() != null && project.get().getState() != message.getProjectState()) {
                return false;
            }
        }
        if (projectBridgehead.isPresent()) {
            if (message.getQueryState() != null
                    && projectBridgehead.get().getExecutions().stream()
                    .noneMatch(execution -> execution.getQueryState() == message.getQueryState())) {
                return false;
            }
            if (message.getProjectBridgeheadState() != null
                    && projectBridgehead.get().getState() != message.getProjectBridgeheadState()) {
                return false;
            }
        }
        if (projectBridgeheadUser.isPresent()) {
            if (message.getProjectRole() != null
                    && projectBridgeheadUser.get().getProjectRole() != message.getProjectRole()) {
                return false;
            }
            if (message.getUserProjectState() != null
                    && projectBridgeheadUser.get().getProjectState() != message.getUserProjectState()) {
                return false;
            }
        }
        return message.getOrganisationRole() == null
                || sessionUser.getUserOrganisationRoles().containsRole(message.getOrganisationRole());
    }
}
