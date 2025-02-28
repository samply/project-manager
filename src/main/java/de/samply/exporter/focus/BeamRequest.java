package de.samply.exporter.focus;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BeamRequest {

    @JsonProperty("body")
    private String body;
    @JsonProperty("failure_strategy")
    private FailureStrategy failureStrategy;
    @JsonProperty("from")
    private String from;
    @JsonProperty("id")
    private String id;
    @JsonProperty("metadata")
    private BeamRequestMetadata metadata;
    @JsonProperty("to")
    private String[] to;
    @JsonProperty("ttl")
    private String ttl;
    @JsonProperty("status")
    private String status;
    @JsonProperty("task")
    private String task;

}
