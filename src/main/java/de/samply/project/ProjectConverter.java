package de.samply.project;

import de.samply.annotations.Bridgehead;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.resolvers.AnnotatedParametersWrapper;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
public class ProjectConverter implements Converter<String, Project> {

    private final ProjectService projectService;
    private final AnnotatedParametersWrapper annotatedParametersWrapper;

    public ProjectConverter(
            ProjectService projectService,
            AnnotatedParametersWrapper annotatedParametersWrapper) {
        this.projectService = projectService;
        this.annotatedParametersWrapper = annotatedParametersWrapper;
    }

    @Override
    public Project convert(@NonNull String projectCode) {
        if (!StringUtils.hasText(projectCode)) return null;
        Optional<ProjectBridgehead> projectBridgeheadOptional =
                annotatedParametersWrapper.getResolved(Bridgehead.class, ProjectBridgehead.class);
        return projectBridgeheadOptional
                .map(ProjectBridgehead::getProject)
                .orElse(projectService.fetchProject(projectCode));
    }

}
