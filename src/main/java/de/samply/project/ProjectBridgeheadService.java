package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.frontend.dto.DtoFactory;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProjectBridgeheadService {

    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final SessionUser sessionUser;

    public ProjectBridgeheadService(ProjectRepository projectRepository,
                                    ProjectBridgeheadRepository projectBridgeheadRepository,
                                    ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                                    SessionUser sessionUser) {
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.sessionUser = sessionUser;
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

    public List<de.samply.frontend.dto.ProjectBridgehead> fetchUserVisibleProjectBridgeheads(@NotNull String projectCode) throws ProjectBridgeheadServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new ProjectBridgeheadServiceException("Project " + projectCode + " not found");
        }
        Set<ProjectBridgehead> projectBridgeheads = projectBridgeheadRepository.findByProject(project.get());
        if (isProjectManagerAdmin()) {
            return new ArrayList<>(projectBridgeheads).stream().map(DtoFactory::convert).toList();
        }
        Set<ProjectBridgehead> tempProjectBridgeheads = new HashSet<>();
        projectBridgeheads.forEach(projectBridgehead -> {
            if (isBridgeheadAdminOfProjectBridgehead(projectBridgehead) || isUserOfProjectBridgehead(projectBridgehead)) {
                tempProjectBridgeheads.add(projectBridgehead);
            }
        });
        return new ArrayList<>(tempProjectBridgeheads).stream().map(DtoFactory::convert).toList();
    }

    private boolean isProjectManagerAdmin() {
        return sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.PROJECT_MANAGER_ADMIN);
    }

    private boolean isBridgeheadAdminOfProjectBridgehead(ProjectBridgehead projectBridgehead) {
        return sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.BRIDGEHEAD_ADMIN, Optional.of(projectBridgehead.getBridgehead()));
    }

    private boolean isUserOfProjectBridgehead(ProjectBridgehead projectBridgehead) {
        if (projectBridgehead.getProject().getCreatorEmail().equals(sessionUser.getEmail())) {
            for (String bridgehead : sessionUser.getBridgeheads()) {
                if (projectBridgehead.getBridgehead().equals(bridgehead)) {
                    return true;
                }
            }
        }
        return !projectBridgeheadUserRepository.getByEmailAndProjectBridgehead(sessionUser.getEmail(), projectBridgehead).isEmpty();
    }

}
