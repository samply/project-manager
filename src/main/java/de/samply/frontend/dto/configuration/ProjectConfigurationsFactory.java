package de.samply.frontend.dto.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import de.samply.form.core.FormConfig;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.Project;
import de.samply.frontend.dto.ProjectOutput;
import de.samply.modules.OptionalModules;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

@Slf4j
@Component
public class ProjectConfigurationsFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public ProjectConfigurations createProjectConfigurations(
            @Value(ProjectManagerConst.FRONTEND_PROJECT_CONFIG_PATH_SV) Path frontendProjectConfigurationPath,
            FormConfig formConfig,
            OptionalModules optionalModules) {
        try {
            ProjectConfigurations configurations = objectMapper.readValue(
                    frontendProjectConfigurationPath.toFile(), ProjectConfigurations.class);
            configurations.validate();
            validateFormReferences(configurations, formConfig);
            validateProjectTypes(configurations, optionalModules);
            return configurations;
        } catch (IOException e) {
            log.error("ProjectCode configuration file not found: {}", frontendProjectConfigurationPath);
            throw new RuntimeException(e);
        }
    }

    // A configuration's form that does not exist would fail at runtime, when a
    // project chooses that configuration.
    private static void validateFormReferences(ProjectConfigurations configurations, FormConfig formConfig) {
        List<String> missing = new ArrayList<>();
        new TreeMap<>(configurations.getConfig()).forEach((name, configuration) ->
                Stream.ofNullable(configuration.forms()).flatMap(Arrays::stream)
                        .map(Form::title)
                        .filter(title -> !formConfig.getFormTitleLabelFieldMap().containsKey(title))
                        .forEach(title -> missing.add("configuration '" + name + "' names form '" + title
                                + "', which does not exist")));
        if (!missing.isEmpty()) {
            missing.forEach(problem -> log.error("Invalid project configuration: {}", problem));
            throw new IllegalStateException(missing.size() + " reference(s) to something not configured: "
                    + String.join("; ", missing));
        }
    }

    // A deployment may only offer request types whose optional modules are enabled: otherwise users could create
    // requests that can never be delivered
    static void validateProjectTypes(ProjectConfigurations configurations, OptionalModules optionalModules) {
        List<String> unavailable = new ArrayList<>();
        new TreeMap<>(configurations.getConfig()).forEach((name, configuration) ->
                Stream.ofNullable(configuration.project()).map(Project::getOutputs).filter(Objects::nonNull)
                        .flatMap(Arrays::stream)
                        .map(ProjectOutput::projectType)
                        .filter(projectType -> projectType != null && !optionalModules.isAvailable(projectType))
                        .distinct()
                        .forEach(projectType -> unavailable.add("configuration '" + name + "': "
                                + optionalModules.describeUnavailable(projectType))));
        if (!unavailable.isEmpty()) {
            unavailable.forEach(problem -> log.error("Invalid project configuration: {}", problem));
            throw new IllegalStateException(unavailable.size() + " request type(s) offered without their modules: "
                    + String.join("; ", unavailable));
        }
    }

}
