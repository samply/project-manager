package de.samply.token.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataShieldStatus(
        @JsonProperty("project_id") String projectCode,
        @JsonProperty("bk") String bridgehead,
        @JsonProperty("user_id") String email,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("project_status") DataShieldProjectStatus projectStatus,
        @JsonProperty("token_status") DataShieldTokenStatus tokenStatus
) {

}
