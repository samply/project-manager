package de.samply.frontend.dto.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.samply.app.ProjectManagerConst;
import de.samply.frontend.dto.Project;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProjectConfigurations {

    private Map<String, Project> config = new HashMap<>();

    @JsonIgnore
    public Map<String, Project> fetchCurrentProjectConfiguration(Project project) {
        return ProjectConfigurationMatcher.fetchMatchProjectConfiguration(project, config);
    }

}
