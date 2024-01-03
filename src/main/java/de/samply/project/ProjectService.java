package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.project.state.ProjectBridgeheadState;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectBridgeheadRepository projectBridgeheadRepository) {
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
    }

    public void editProject(@NotNull String projectCode, ProjectType type, String[] bridgeheads) {
        Optional<Project> projectOptional = this.projectRepository.findByCode(projectCode);
        boolean hasChanged = false;
        if (projectOptional.isPresent()) {
            if (type != null) {
                projectOptional.get().setType(type);
                hasChanged = true;
            }
            if (hasChanged) {
                projectOptional.get().setModifiedAt(Instant.now());
                projectRepository.save(projectOptional.get());
            }
            if (bridgeheads != null && bridgeheads.length > 0) {
                updateBridgeheads(projectOptional.get(), bridgeheads);
            }
        }
    }

    private void updateBridgeheads(Project project, String[] bridgeheads) {
        Set<String> editionBridgeheads = Set.of(bridgeheads);
        // Remove bridgeheads that are no longer present
        projectBridgeheadRepository.findByProject(project).stream().filter(projectBridgehead ->
                !editionBridgeheads.contains(projectBridgehead.getBridgehead())).forEach(projectBridgehead ->
                projectBridgeheadRepository.delete(projectBridgehead));
        // Add new bridgeheads
        Set<String> oldBridgeheads = new HashSet<>(projectBridgeheadRepository.findByProject(project).stream().
                map(projectBridgehead -> projectBridgehead.getBridgehead()).toList());
        editionBridgeheads.stream().filter(bridgehead -> !oldBridgeheads.contains(bridgehead)).forEach(bridgehead ->
                createProjectBridgehead(project, bridgehead));
    }

    private void createProjectBridgehead(Project project, String bridgehead) {
        ProjectBridgehead projectBridgehead = new ProjectBridgehead();
        projectBridgehead.setBridgehead(bridgehead);
        projectBridgehead.setProject(project);
        projectBridgehead.setState(ProjectBridgeheadState.CREATED);
        projectBridgehead.setModifiedAt(Instant.now());
        projectBridgeheadRepository.save(projectBridgehead);
    }

}
