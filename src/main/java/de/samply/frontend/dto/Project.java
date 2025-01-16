package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.annotations.IgnoreProjectConfigurationMatch;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectState;
import de.samply.project.state.UserProjectState;
import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Project {

    @IgnoreProjectConfigurationMatch
    private String code;
    @IgnoreProjectConfigurationMatch
    private String creatorEmail;
    @IgnoreProjectConfigurationMatch
    private String creatorName;
    @IgnoreProjectConfigurationMatch
    private Instant createdAt;
    private LocalDate expiresAt;
    @IgnoreProjectConfigurationMatch
    private Instant archivedAt;
    @IgnoreProjectConfigurationMatch
    private Instant modifiedAt;
    private ProjectState state;
    private ProjectType type;
    private String query;
    private String humanReadable;
    private QueryFormat queryFormat;
    private OutputFormat outputFormat;
    private String templateId;
    @IgnoreProjectConfigurationMatch
    private String label;
    @IgnoreProjectConfigurationMatch
    private String description;
    @IgnoreProjectConfigurationMatch
    private String explorerUrl;
    private String queryContext;
    private boolean isCustomConfig;
    private UserProjectState creatorState;

}
