package de.samply.frontend.dto.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.Base64Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ProjectConfigurationsFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public ProjectConfigurations createProjectConfigurations(@Value(ProjectManagerConst.FRONTEND_PROJECT_CONFIG_SV) String frontendProjectConfigurations) throws JsonProcessingException {
        ProjectConfigurations projectConfigurations = objectMapper.readValue(Base64Utils.decodeIfNecessary(frontendProjectConfigurations), ProjectConfigurations.class);
        projectConfigurations.initConfigurationNameProjectMap();
        return projectConfigurations;
    }

}
