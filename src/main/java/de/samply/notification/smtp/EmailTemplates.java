package de.samply.notification.smtp;

import de.samply.app.ProjectManagerConst;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = ProjectManagerConst.THYMELEAF_TEMPLATES)
@Data
public class EmailTemplates {

    private Map<ProjectRole, String> invitation = new HashMap<>();
    private Map<ProjectRole, String> newProject = new HashMap<>();

    public Optional<String> getTemplate(@NotNull EmailTemplateType type, @NotNull ProjectRole role) {
        return getTemplate(role, fetchMap(type));
    }

    private Map<ProjectRole, String> fetchMap(EmailTemplateType type) {
        return switch (type) {
            case INVITATION -> invitation;
            case NEW_PROJECT -> newProject;
        };
    }

    private Optional<String> getTemplate(ProjectRole role, Map<ProjectRole, String> roleTemplateMap) {
        String template = roleTemplateMap.get(role);
        return Optional.ofNullable((template != null) ? template : roleTemplateMap.get(ProjectRole.DEFAULT));
    }

}
