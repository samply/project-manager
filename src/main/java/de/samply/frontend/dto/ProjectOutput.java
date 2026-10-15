package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.project.ProjectType;
import de.samply.query.OutputFormat;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectOutput(
        ProjectType projectType,
        OutputFormat outputFormat,
        String templateId) {
}
