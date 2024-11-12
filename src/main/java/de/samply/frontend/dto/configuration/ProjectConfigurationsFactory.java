package de.samply.frontend.dto.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.Base64Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProjectConfigurationsFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public ProjectConfigurations createProjectConfigurations(@Value(ProjectManagerConst.FRONTEND_PROJECT_CONFIG_SV) String frontendProjectConfigurations) throws JsonProcessingException {
        Optional<String> decodedFrontendConfigurations = Base64Utils.decodeIfNecessary(frontendProjectConfigurations);
        if (decodedFrontendConfigurations.isEmpty()) {
            throw new RuntimeException("No frontend configurations found");
        }
        ProjectConfigurations projectConfigurations = objectMapper.readValue(decodedFrontendConfigurations.get(), ProjectConfigurations.class);
        projectConfigurations.initConfigurationNameProjectMap();
        return projectConfigurations;
    }

}
