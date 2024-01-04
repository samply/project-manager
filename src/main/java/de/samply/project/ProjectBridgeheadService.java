package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.project.state.ProjectBridgeheadState;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProjectBridgeheadService {

    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;

    public ProjectBridgeheadService(ProjectRepository projectRepository,
                                    ProjectBridgeheadRepository projectBridgeheadRepository) {
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
    }

    public void acceptProject(@NotNull String projectCode, @NotNull String bridgehead) throws ProjectBridgeheadServiceException {
        changeProjectBridgeheadState(projectCode, bridgehead, ProjectBridgeheadState.ACCEPTED);
    }

    public void rejectProject(@NotNull String projectCode, @NotNull String bridgehead) throws ProjectBridgeheadServiceException {
        changeProjectBridgeheadState(projectCode, bridgehead, ProjectBridgeheadState.REJECTED);
    }

    private void changeProjectBridgeheadState(@NotNull String projectCode, @NotNull String bridgehead, @NotNull ProjectBridgeheadState state) throws ProjectBridgeheadServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new ProjectBridgeheadServiceException("Project not found: " + projectCode);
        }
        Optional<ProjectBridgehead> projectBridgehead = projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project.get());
        if (projectBridgehead.isEmpty()) {
            throw new ProjectBridgeheadServiceException("Bridghead " + bridgehead + " in project " + projectCode + " not found");
        }
        projectBridgehead.get().setState(state);
        projectBridgeheadRepository.save(projectBridgehead.get());
    }

}
