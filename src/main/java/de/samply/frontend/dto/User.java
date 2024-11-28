package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.project.state.UserProjectState;
import de.samply.user.roles.ProjectRole;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record User(
        String email,
        String firstName,
        String lastName,
        String bridgehead,
        String humanReadableBridgehead,
        ProjectRole projectRole,
        UserProjectState projectState
) {
}
