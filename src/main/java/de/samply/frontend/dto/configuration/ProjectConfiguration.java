package de.samply.frontend.dto.configuration;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProjectConfiguration {
    private Map<String, String> fieldValues = new HashMap<>();
    private String title;
    private String description;
}
