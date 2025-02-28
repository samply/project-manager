package de.samply.coder;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.coder.request.CreateRequestBody;
import de.samply.project.ProjectType;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = ProjectManagerConst.CODER_PREFIX)
public class CoderConfiguration {

    private final ObjectMapper objectMapper = new ObjectMapper(); // For deserializing JSON
    private Map<String, String> path;
    private Map<ProjectType, CreateRequestBody> projectTypeCreateRequestBodyMap = new EnumMap<>(ProjectType.class);

    @PostConstruct
    public void init() {
        path.forEach((key, value) -> {
            ProjectType projectType = mapToProjectType(key); // Map the string key to ProjectType
            if (projectType != null) {
                try {
                    // Deserialize JSON file into CreateRequestBody
                    CreateRequestBody requestBody = objectMapper.readValue(new File(value), CreateRequestBody.class);
                    projectTypeCreateRequestBodyMap.put(projectType, requestBody);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Failed to parse JSON file for key: " + key, e);
                }
            }
        });
    }

    private ProjectType mapToProjectType(String key) {
        try {
            // Convert key to uppercase and replace underscores for enum matching
            return ProjectType.valueOf(key.replace(ProjectManagerConst.UNDERSCORE, "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // Return null if the key doesn't map to a ProjectType
        }
    }

    public CreateRequestBody cloneCreateRequestBody(ProjectType projectType) {
        CreateRequestBody original = projectTypeCreateRequestBodyMap.get(projectType);
        if (original == null) {
            throw new IllegalArgumentException("No CreateRequestBody found for ProjectType: " + projectType);
        }
        return original.toBuilder().build(); // Clone using Lombok's toBuilder
    }

}
