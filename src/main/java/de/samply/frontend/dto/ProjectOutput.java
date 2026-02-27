package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.project.ProjectType;
import de.samply.query.OutputFormat;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ProjectOutput {
    private ProjectType projectType;
    private OutputFormat outputFormat;
    private String templateId;

    public ProjectOutput(ProjectType projectType, OutputFormat outputFormat, String templateId) {
        this.projectType = projectType;
        this.outputFormat = outputFormat;
        this.templateId = templateId;
    }

}
