package de.samply.frontend.dto;

import de.samply.project.state.UserProjectState;
import de.samply.user.roles.ProjectRole;

public record User(
        String email,
        String bridgehead,
        ProjectRole projectRole,
        UserProjectState projectState
) {
}
