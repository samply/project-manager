package de.samply.datashield.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.app.ProjectManagerConst;

public record DataShieldTokenManagerTokenStatus(
        @JsonProperty(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_CODE) String projectCode,
        @JsonProperty(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_BRIDGEHEAD) String bridgehead,
        @JsonProperty(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_EMAIL) String email,
        @JsonProperty(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_TOKEN_CREATED_AT) String createdAt,
        @JsonProperty(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_PROJECT_STATUS) DataShieldProjectStatus projectStatus,
        @JsonProperty(ProjectManagerConst.TOKEN_MANAGER_PARAMETER_TOKEN_STATUS) DataShieldTokenStatus tokenStatus
) {
}
