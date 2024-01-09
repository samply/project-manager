package de.samply.exporter.focus;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FocusQueryMetadata {

    @JsonProperty("project")
    private String project;
    @JsonProperty("execute")
    private boolean execute;

}
