package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.UserProjectState;
import de.samply.query.QueryState;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectBridgehead(
        String projectCode,
        String bridgehead,
        String humanReadable,
        ProjectBridgeheadState state,
        Instant modifiedAt,
        QueryState queryState,
        UserProjectState creatorState
) {
}
