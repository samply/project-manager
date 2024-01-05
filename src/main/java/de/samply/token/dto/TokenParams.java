package de.samply.token.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TokenParams(
        @JsonProperty("email") String email,
        @JsonProperty("project_id") String projectId,
        @JsonProperty("bridgehead_ids") List<String> bridgeheadIds
) {
}
