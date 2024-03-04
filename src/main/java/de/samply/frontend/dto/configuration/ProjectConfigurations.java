package de.samply.frontend.dto.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.samply.app.ProjectManagerConst;
import de.samply.frontend.dto.Project;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProjectConfigurations {

    private Map<String, ProjectConfiguration> config = new HashMap<>();
    @JsonIgnore
    private Map<String, Project> configurationNameProjectMap = new HashMap<>();

    @JsonIgnore
    public void initConfigurationNameProjectMap() {
        config.forEach((configurationName, projectConfiguration) -> configurationNameProjectMap.put(configurationName, convert(projectConfiguration)));
        configurationNameProjectMap.put(ProjectManagerConst.CUSTOM_PROJECT_CONFIGURATION, new Project());
    }

    @JsonIgnore
    private static Project convert(ProjectConfiguration projectConfiguration) {
        Project result = new Project();
        result.setFieldsFromMap(projectConfiguration.getFieldValues());
        return result;
    }

    @JsonIgnore
    public Map<String, Project> fetchCurrentProjectConfiguration(Project project) {
        return ProjectConfigurationMatcher.fetchMatchProjectConfiguration(project, configurationNameProjectMap);
    }

}
