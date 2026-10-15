package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeasibilityItem(
        String label,
        Long value,
        List<FeasibilityItem> breakdown
) {
}
