package de.samply.frontend.dto.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class ProjectConfigurationsFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public ProjectConfigurations createProjectConfigurations(@Value(ProjectManagerConst.FRONTEND_PROJECT_CONFIG_PATH_SV) String frontendProjectConfigurationPath) {
        try {
            ProjectConfigurations projectConfigurations = objectMapper.readValue(new File(frontendProjectConfigurationPath), ProjectConfigurations.class);
            return projectConfigurations;
        } catch (IOException e) {
            log.error("Project configuration file not found: {}", frontendProjectConfigurationPath);
            throw new RuntimeException(e);
        }
    }

}
