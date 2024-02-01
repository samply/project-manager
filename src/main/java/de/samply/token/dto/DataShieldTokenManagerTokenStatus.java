package de.samply.token.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataShieldTokenManagerTokenStatus(
        @JsonProperty("project_id") String projectCode,
        @JsonProperty("bk") String bridgehead,
        @JsonProperty("user_id") String email,
        @JsonProperty("token_created_at") String createdAt,
        @JsonProperty("project_status") DataShieldProjectStatus projectStatus,
        @JsonProperty("token_status") DataShieldTokenStatus tokenStatus
) {
}
