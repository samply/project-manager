package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectState;
import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Project {

    private String code;
    private String creatorEmail;
    private String creatorName;
    private Instant createdAt;
    private LocalDate expiresAt;
    private Instant archivedAt;
    private Instant modifiedAt;
    private ProjectState state;
    private ProjectType type;
    private String query;
    private String humanReadable;
    private QueryFormat queryFormat;
    private OutputFormat outputFormat;
    private String templateId;
    private String label;
    private String description;
    private String explorerUrl;
    private String queryContext;
    private boolean isCustomConfig;

}
