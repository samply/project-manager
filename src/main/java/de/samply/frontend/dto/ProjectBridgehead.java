package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.UserProjectState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectBridgehead extends Bridgehead {
    private final String projectCode;
    private final ProjectBridgeheadState state;
    private final Instant modifiedAt;
    private final ProjectBridgeheadExecution[] executions;
    private final UserProjectState creatorState;

    public ProjectBridgehead(String projectCode, String bridgehead, String humanReadable,
                             BridgeheadContact[] contacts, ProjectBridgeheadState state,
                             Instant modifiedAt, ProjectBridgeheadExecution[] executions,
                             UserProjectState creatorState) {
        super(bridgehead, humanReadable, contacts);
        this.projectCode = projectCode;
        this.state = state;
        this.modifiedAt = modifiedAt;
        this.executions = executions;
        this.creatorState = creatorState;
    }
}
