package de.samply.frontend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.security.SessionUser;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionExplanations {

    @JsonProperty("explanations")
    private Map<String, List<ActionExplanation>> actionExplanationMap;

    public Optional<List<ActionExplanation>> getActionExplanation(String action) {
        return Optional.ofNullable(actionExplanationMap.get(action));
    }

    public Optional<Pair<String, Integer>> fetchExplanationAndPriority(@NotNull String action, @NotNull String module, @NotNull String language,
                                                             Optional<Project> project, Optional<ProjectBridgehead> projectBridgehead,
                                                             Optional<ProjectBridgeheadUser> projectBridgeheadUser, SessionUser sessionUser) {
        Optional<List<ActionExplanation>> actionExplanations = getActionExplanation(action);
        if (actionExplanations.isPresent()) {
            for (ActionExplanation explanation : actionExplanations.get()) {
                if (isRequiredExplanation(module, project, projectBridgehead, projectBridgeheadUser, sessionUser, explanation)) {
                    String message = explanation.getLanguageMessageMap().get(language);
                    if (message != null) {
                        return Optional.of(Pair.of(message, explanation.getPriority() != null ? explanation.getPriority() : 0));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean isRequiredExplanation(String module, Optional<Project> project, Optional<ProjectBridgehead> projectBridgehead, Optional<ProjectBridgeheadUser> projectBridgeheadUser, SessionUser sessionUser, ActionExplanation explanation) {
        if (module != null && explanation.getModule() != null && !module.equalsIgnoreCase(explanation.getModule())) {
            return false;
        }
        if (project.isPresent()) {
            if (explanation.getProjectType() != null && project.get().getType() != explanation.getProjectType()) {
                return false;
            }
            if (explanation.getProjectState() != null && project.get().getState() != explanation.getProjectState()) {
                return false;
            }
        }
        if (projectBridgehead.isPresent()) {
            if (explanation.getQueryState() != null && projectBridgehead.get().getQueryState() != explanation.getQueryState()) {
                return false;
            }
            if (explanation.getProjectBridgeheadState() != null && projectBridgehead.get().getState() != explanation.getProjectBridgeheadState()) {
                return false;
            }
        }
        if (projectBridgeheadUser.isPresent()) {
            if (explanation.getProjectRole() != null && projectBridgeheadUser.get().getProjectRole() != explanation.getProjectRole()) {
                return false;
            }
            if (explanation.getUserProjectState() != null && projectBridgeheadUser.get().getProjectState() != explanation.getUserProjectState()) {
                return false;
            }
        }
        if (explanation.getOrganisationRole() != null && !sessionUser.getUserOrganisationRoles().containsRole(explanation.getOrganisationRole())) {
            return false;
        }
        return true;
    }

}
