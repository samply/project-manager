package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.UserProjectState;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Results(
        String bridgehead,
        String humanReadableBridgehead,
        String email,
        String firstName,
        String lastName,
        String url,
        UserProjectState creatorState,
        ProjectBridgeheadState bridgeheadAdminState,
        UserProjectState finalUserState
) {
}
