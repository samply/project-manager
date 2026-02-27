package de.samply.exporter;

import de.samply.db.model.ProjectBridgehead;
import de.samply.project.ProjectType;

import java.util.stream.Stream;

public record ProjectBridgeheadAndType(ProjectBridgehead projectBridgehead, ProjectType projectType) {

    public static Stream<ProjectBridgeheadAndType> from(ProjectBridgehead projectBridgehead) {
        return projectBridgehead.getExecutions().stream().map(execution ->
                new ProjectBridgeheadAndType(projectBridgehead, execution.getQueryOutput().getProjectType()));
    }
}
