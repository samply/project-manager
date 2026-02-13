package de.samply.coder.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// This class can be instantiated with CoderConfiguration with ObjectMapper
@SuppressWarnings("unused")
@Data
public class Build {

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("workspace_id")
    private String workspaceId;

}
