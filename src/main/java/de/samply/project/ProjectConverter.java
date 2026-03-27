package de.samply.project;

import de.samply.db.model.Project;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProjectConverter implements Converter<String, Project> {

    private final ProjectService projectService;

    public ProjectConverter(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public Project convert(@NonNull String projectCode) {
        return projectService.fetchProject(projectCode);
    }

}
