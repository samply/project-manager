package de.samply.datashield.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TokenParams(
        @JsonProperty("user_id") String email,
        @JsonProperty("project_id") String projectId,
        @JsonProperty("bridgehead_ids") List<String> bridgeheadIds
) {
}
