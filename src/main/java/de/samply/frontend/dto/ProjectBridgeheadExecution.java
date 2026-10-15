package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.project.ProjectType;
import de.samply.query.QueryState;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectBridgeheadExecution(
        ProjectType projectType,
        QueryState queryState) {

}
