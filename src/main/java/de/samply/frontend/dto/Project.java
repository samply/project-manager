package de.samply.frontend.dto;

import de.samply.project.ProjectType;
import de.samply.project.state.ProjectState;
import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;

import java.time.Instant;
import java.time.LocalDate;

public record Project(
        String code,
        String creatorEmail,
        Instant createdAt,
        LocalDate expiresAt,
        Instant archivedAt,
        Instant modifiedAt,
        ProjectState state,
        ProjectType type,
        String query,
        String humanReadable,
        QueryFormat queryFormat,
        OutputFormat outputFormat,
        String templateId,
        String label,
        String description,
        String explorerUrl,
        String queryContext
) {
}
