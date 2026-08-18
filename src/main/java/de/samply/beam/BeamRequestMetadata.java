package de.samply.beam;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BeamRequestMetadata {

    @JsonProperty("project")
    private String project;
    @JsonProperty("task_type")
    private TaskType taskType;
    @JsonProperty("transform")
    private String transform;

}
