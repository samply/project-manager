package de.samply.coder.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Response {

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("id")
    private String id;

    @JsonProperty("latest_build")
    private Build latestBuild;

    @JsonProperty("status")
    private String status;


}
