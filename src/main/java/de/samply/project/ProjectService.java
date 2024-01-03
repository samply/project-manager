package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final SessionUser sessionUser;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectBridgeheadRepository projectBridgeheadRepository,
                          SessionUser sessionUser) {
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.sessionUser = sessionUser;
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

    public Page<Project> fetchUserVisibleProjects(
            Optional<ProjectState> projectState, Optional<Boolean> archived, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        if (isProjectManagerAdmin()) {
            return fetchProjectManagerAdminProjects(projectState, archived, pageRequest);
        }
        Set<String> bridgeheads = sessionUser.getBridgeheads();
        // We make an assumption: A bridgehead admin is bridgehead admin in all of their bridgeheads.
        if (isBridgeheadAdmin()) {
            return fetchBridgeheadAdminProjects(bridgeheads, projectState, archived, pageRequest);
        }
        return fetchResearcherProjects(sessionUser.getEmail(), bridgeheads, projectState, archived, pageRequest);
    }

    private boolean isProjectManagerAdmin() {
        return sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.PROJECT_MANAGER_ADMIN);
    }

    private boolean isBridgeheadAdmin() {
        for (String bridgehead : sessionUser.getBridgeheads()) {
            if (sessionUser.getUserOrganisationRoles()
                    .getBridgeheadRoles(bridgehead).contains(OrganisationRole.BRIDGEHEAD_ADMIN)) {
                return true;
            }
        }
        return false;
    }

    private Page<Project> fetchProjectManagerAdminProjects(
            Optional<ProjectState> projectState, Optional<Boolean> archived, PageRequest pageRequest) {
        if (projectState.isEmpty()) {
            if (archived.isEmpty()) {
                return projectRepository.findAll(pageRequest);
            } else {
                return (archived.get()) ? projectRepository.findAllArchivedProjects(pageRequest) :
                        projectRepository.findAllNotArchivedProjects(pageRequest);
            }
        } else {
            if (archived.isEmpty()) {
                return projectRepository.findByState(projectState.get(), pageRequest);
            } else {
                return (archived.get()) ? projectRepository.findArchivedProjectsByState(projectState.get(), pageRequest) :
                        projectRepository.findNotArchivedProjectsByState(projectState.get(), pageRequest);
            }
        }
    }

    private Page<Project> fetchBridgeheadAdminProjects(
            Set<String> bridgeheads, Optional<ProjectState> projectState, Optional<Boolean> archived, PageRequest pageRequest) {
        if (projectState.isEmpty()) {
            if (archived.isEmpty()) {
                return projectRepository.findByBridgeheads(bridgeheads, pageRequest);
            } else {
                return (archived.get()) ? projectRepository.findArchivedProjectsByBridgeheads(bridgeheads, pageRequest) :
                        projectRepository.findNotArchivedProjectsByBridgeheads(bridgeheads, pageRequest);
            }
        } else {
            if (archived.isEmpty()) {
                return projectRepository.findByStateAndBridgeheads(projectState.get(), bridgeheads, pageRequest);
            } else {
                return (archived.get()) ? projectRepository.findArchivedProjectsByStateAndBridgeheads(projectState.get(), bridgeheads, pageRequest) :
                        projectRepository.findNotArchivedProjectsByStateAndBridgeheads(projectState.get(), bridgeheads, pageRequest);
            }
        }
    }

    private Page<Project> fetchResearcherProjects(String email, Set<String> bridgeheads, Optional<ProjectState> projectState,
                                                  Optional<Boolean> archived, PageRequest pageRequest) {
        if (projectState.isEmpty()) {
            if (archived.isEmpty()) {
                return projectRepository.findByEmailAndBridgeheads(email, bridgeheads, pageRequest);
            } else {
                return (archived.get()) ? projectRepository.findArchivedProjectsByEmailAndBridgeheads(email, bridgeheads, pageRequest) :
                        projectRepository.findNotArchivedProjectsByEmailAndBridgeheads(email, bridgeheads, pageRequest);
            }
        } else {
            if (archived.isEmpty()) {
                return projectRepository.findByEmailAndStateAndBridgeheads(email, projectState.get(), bridgeheads, pageRequest);
            } else {
                return (archived.get()) ? projectRepository.findArchivedProjectsByEmailAndStateAndBridgeheads(email, projectState.get(), bridgeheads, pageRequest) :
                        projectRepository.findNotArchivedProjectsByEmailAndStateAndBridgeheads(email, projectState.get(), bridgeheads, pageRequest);
            }
        }
    }

}
