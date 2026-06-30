package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormFieldValue(
        String label,
        String displayName,
        String description) {
}
