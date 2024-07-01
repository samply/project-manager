package de.samply.exporter.focus;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BeamRequestMetadata {

    @JsonProperty("project")
    private String project;
    @JsonProperty("task_type")
    private TaskType taskType;

}
