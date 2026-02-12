package de.samply.frontend.dto.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.samply.frontend.dto.ProjectAndForms;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProjectConfigurations {

    private Map<String, ProjectAndForms> config = new HashMap<>();

    @JsonIgnore
    public Map<String, ProjectAndForms> fetchCurrentProjectConfiguration(ProjectAndForms projectAndForms) {
        return ProjectConfigurationMatcher.fetchMatchProjectConfiguration(projectAndForms, config);
    }

}

