package de.samply.frontend.dto;

import de.samply.project.state.ProjectBridgeheadState;
import de.samply.query.QueryState;

import java.time.Instant;

public record ProjectBridgehead(
        String projectCode,
        String bridgehead,
        ProjectBridgeheadState state,
        Instant modifiedAt,
        QueryState queryState
) {
}
