package de.samply.exporter.focus;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FocusQuery {

    @JsonProperty("body")
    private String body;
    @JsonProperty("failure_strategy")
    private FailureStrategy failureStrategy;
    @JsonProperty("from")
    private String from;
    @JsonProperty("id")
    private String id;
    @JsonProperty("metadata")
    private FocusQueryMetadata metadata;
    @JsonProperty("to")
    private String[] to;
    @JsonProperty("ttl")
    private String ttl;

}
