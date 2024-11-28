package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Bridgehead(
        String bridgehead,
        String humanReadable
) {
}
