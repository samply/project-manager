package de.samply.project;

import de.samply.annotations.ProjectCode;
import de.samply.batch.ActionsBatchLookups;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.resolvers.AnnotatedParametersWrapper;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
public class ProjectBridgeheadConverter implements Converter<String, ProjectBridgehead> {

    private final AnnotatedParametersWrapper annotatedParametersWrapper;
    private final ProjectBridgeheadService projectBridgeheadService;

    public ProjectBridgeheadConverter(
            AnnotatedParametersWrapper annotatedParametersWrapper,
            ProjectBridgeheadService projectBridgeheadService) {
        this.annotatedParametersWrapper = annotatedParametersWrapper;
        this.projectBridgeheadService = projectBridgeheadService;
    }

    @Override
    public ProjectBridgehead convert(@NonNull String bridgehead) {

        if (!StringUtils.hasText(bridgehead)) return null;

        // Get the resolved ProjectCode if available
        Optional<Project> projectOpt = annotatedParametersWrapper.getResolved(ProjectCode.class, Project.class);

        // Get the raw project code if no ProjectCode is resolved
        String projectCode = projectOpt.map(Project::getCode)
                .or(() -> annotatedParametersWrapper.getRaw(ProjectCode.class, String.class))
                .orElseThrow(() -> new IllegalArgumentException("ProjectCode code not found"));

        // In an actions batch, once per batch
        return ActionsBatchLookups.current()
                .map(lookups -> lookups.bridgehead(projectCode, bridgehead, () -> load(projectCode, bridgehead)))
                .orElseGet(() -> load(projectCode, bridgehead));
    }

    private ProjectBridgehead load(String projectCode, String bridgehead) {
        return projectBridgeheadService
                .fetchProjectBridgehead(projectCode, bridgehead)
                .orElseThrow(() -> new IllegalArgumentException("Bridgehead " + bridgehead + " not found for project " + projectCode));
    }

}
