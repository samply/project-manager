package de.samply.token.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.app.ProjectManagerConst;

public record DataShieldTokenManagerProjectStatus(
        @JsonProperty(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_CODE) String projectCode,
        @JsonProperty(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_BRIDGEHEAD) String bridgehead,
        @JsonProperty(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_STATUS) DataShieldProjectStatus projectStatus
        ) {
}
