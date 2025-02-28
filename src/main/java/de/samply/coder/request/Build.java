package de.samply.coder.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Build {

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("workspace_id")
    private String workspaceId;

}
