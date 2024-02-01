package de.samply.token.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataShieldTokenManagerProjectStatus(
        @JsonProperty("project_id") String projectCode,
        @JsonProperty("bk") String bridgehead,
        @JsonProperty("project_status") DataShieldProjectStatus projectStatus
        ) {
}
