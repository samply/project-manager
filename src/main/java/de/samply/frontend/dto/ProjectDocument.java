package de.samply.frontend.dto;

import de.samply.document.DocumentType;

import java.time.Instant;

public record ProjectDocument(
        String projectCode,
        String originalFilename,
        String url,
        Instant createdAt,
        String bridgehead,
        String creatorEmail,
        String label,
        DocumentType type
) {

}
