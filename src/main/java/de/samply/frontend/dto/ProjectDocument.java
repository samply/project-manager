package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.document.DocumentType;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectDocument(
        String projectCode,
        String originalFilename,
        String url,
        Instant createdAt,
        String bridgehead,
        String humanReadableBridgehead,
        String creatorEmail,
        String label,
        DocumentType type
) {

}
