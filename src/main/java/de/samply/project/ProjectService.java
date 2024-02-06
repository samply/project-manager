package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.frontend.dto.DtoFactory;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProjectService {

    private final NotificationService notificationService;
    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final SessionUser sessionUser;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;

    public ProjectService(NotificationService notificationService,
                          ProjectRepository projectRepository,
                          ProjectBridgeheadRepository projectBridgeheadRepository,
                          SessionUser sessionUser,
                          ProjectBridgeheadUserRepository projectBridgeheadUserRepository) {
        this.notificationService = notificationService;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.sessionUser = sessionUser;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
    }

    public de.samply.frontend.dto.Project fetchProject(@NotNull String projectCode) throws ProjectServiceException {
        Optional<Project> projectOptional = this.projectRepository.findByCode(projectCode);
        if (projectOptional.isEmpty()) {
            throw new ProjectServiceException("Project " + projectCode + " not found");
        }
        return DtoFactory.convert(projectOptional.get());
    }

    public void editProject(@NotNull String projectCode, ProjectType type, String[] bridgeheads) {
        Optional<Project> projectOptional = this.projectRepository.findByCode(projectCode);
        boolean hasChanged = false;
        if (projectOptional.isPresent()) {
            if (type != null) {
                projectOptional.get().setType(type);
                this.notificationService.createNotification(projectCode, null, sessionUser.getEmail(),
                        OperationType.EDIT_PROJECT, "Changed project type to " + type, null, null);
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
        this.notificationService.createNotification(project.getCode(), null, sessionUser.getEmail(),
                OperationType.EDIT_PROJECT, "Changed bridgeheads: " + String.join("," + bridgeheads), null, null);

    }

    private void createProjectBridgehead(Project project, String bridgehead) {
        ProjectBridgehead projectBridgehead = new ProjectBridgehead();
        projectBridgehead.setBridgehead(bridgehead);
        projectBridgehead.setProject(project);
        projectBridgehead.setState(ProjectBridgeheadState.CREATED);
        projectBridgehead.setModifiedAt(Instant.now());
        projectBridgeheadRepository.save(projectBridgehead);
    }

    public List<Project> fetchAllUserVisibleProjects() {
        // Fetch projects as project manager
        if (isProjectManagerAdmin()) {
            return projectRepository.findAll();
        }
        Set<String> bridgeheads = sessionUser.getBridgeheads();
        // Fetch projects as bridgehead admin
        // We make an assumption: A bridgehead admin is bridgehead admin in all of their bridgeheads.
        if (isBridgeheadAdmin()) {
            return projectRepository.findByBridgeheads(bridgeheads);
        }
        // Fetch projects as researcher
        return projectBridgeheadUserRepository.findProjectsByEmail(sessionUser.getEmail());
    }

    public Page<de.samply.frontend.dto.Project> fetchUserVisibleProjects(
            Optional<ProjectState> projectState, Optional<Boolean> archived, int page, int pageSize,
            boolean modifiedDescendant) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        if (isProjectManagerAdmin()) {
            return fetchProjectManagerAdminProjects(projectState, archived, pageRequest, modifiedDescendant).map(DtoFactory::convert);
        }
        Set<String> bridgeheads = sessionUser.getBridgeheads();
        // We make an assumption: A bridgehead admin is bridgehead admin in all of their bridgeheads.
        if (isBridgeheadAdmin()) {
            return fetchBridgeheadAdminProjects(bridgeheads, projectState, archived, pageRequest, modifiedDescendant).map(DtoFactory::convert);
        }
        return fetchResearcherProjects(sessionUser.getEmail(), bridgeheads, projectState, archived, pageRequest, modifiedDescendant).map(DtoFactory::convert);
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
            Optional<ProjectState> projectState, Optional<Boolean> archived, PageRequest pageRequest,
            boolean modifiedDescendant) {
        if (projectState.isEmpty()) {
            if (archived.isEmpty()) {
                if (modifiedDescendant) {
                    return projectRepository.findAllByOrderByModifiedAtDesc(pageRequest);
                } else {
                    return projectRepository.findAllByOrderByModifiedAtAsc(pageRequest);
                }
            } else {
                if (archived.get()) {
                    if (modifiedDescendant) {
                        return projectRepository.findAllArchivedProjectsModifiedAtDesc(pageRequest);
                    } else {
                        return projectRepository.findAllArchivedProjectsModifiedAtAsc(pageRequest);
                    }
                } else {
                    if (modifiedDescendant) {
                        return projectRepository.findAllNotArchivedProjectsModifiedAtDesc(pageRequest);
                    } else {
                        return projectRepository.findAllNotArchivedProjectsModifiedAtAsc(pageRequest);
                    }
                }
            }
        } else {
            if (archived.isEmpty()) {
                if (modifiedDescendant) {
                    return projectRepository.findByStateOrderByModifiedAtDesc(projectState.get(), pageRequest);
                } else {
                    return projectRepository.findByStateOrderByModifiedAtAsc(projectState.get(), pageRequest);
                }
            } else {
                if (archived.get()) {
                    if (modifiedDescendant) {
                        return projectRepository.findArchivedProjectsByStateModifiedAtDesc(projectState.get(), pageRequest);
                    } else {
                        return projectRepository.findArchivedProjectsByStateModifiedAtAsc(projectState.get(), pageRequest);
                    }
                } else {
                    if (modifiedDescendant) {
                        return projectRepository.findNotArchivedProjectsByStateModifiedAtDesc(projectState.get(), pageRequest);
                    } else {
                        return projectRepository.findNotArchivedProjectsByStateModifiedAtAsc(projectState.get(), pageRequest);
                    }
                }
            }
        }
    }

    private Page<Project> fetchBridgeheadAdminProjects(
            Set<String> bridgeheads, Optional<ProjectState> projectState, Optional<Boolean> archived,
            PageRequest pageRequest, boolean modifiedDescendant) {
        if (projectState.isEmpty()) {
            if (archived.isEmpty()) {
                if (modifiedDescendant) {
                    return projectRepository.findByBridgeheadsModifiedAtDesc(bridgeheads, pageRequest);
                } else {
                    return projectRepository.findByBridgeheadsModifiedAtAsc(bridgeheads, pageRequest);
                }
            } else {
                if (archived.get()) {
                    if (modifiedDescendant) {
                        return projectRepository.findArchivedProjectsByBridgeheadsModifiedAtDesc(bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findArchivedProjectsByBridgeheadsModifiedAtAsc(bridgeheads, pageRequest);
                    }
                } else {
                    if (modifiedDescendant) {
                        return projectRepository.findNotArchivedProjectsByBridgeheadsModifiedAtDesc(bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findNotArchivedProjectsByBridgeheadsModifiedAtAsc(bridgeheads, pageRequest);
                    }
                }
            }
        } else {
            if (archived.isEmpty()) {
                if (modifiedDescendant) {
                    return projectRepository.findByStateAndBridgeheadsModifiedAtDesc(projectState.get(), bridgeheads, pageRequest);
                } else {
                    return projectRepository.findByStateAndBridgeheadsModifiedAtAsc(projectState.get(), bridgeheads, pageRequest);
                }
            } else {
                if (archived.get()) {
                    if (modifiedDescendant) {
                        return projectRepository.findArchivedProjectsByStateAndBridgeheadsModifiedAtDesc(projectState.get(), bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findArchivedProjectsByStateAndBridgeheadsModifiedAtAsc(projectState.get(), bridgeheads, pageRequest);
                    }
                } else {
                    if (modifiedDescendant) {
                        return projectRepository.findNotArchivedProjectsByStateAndBridgeheadsModifiedAtDesc(projectState.get(), bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findNotArchivedProjectsByStateAndBridgeheadsModifiedAtAsc(projectState.get(), bridgeheads, pageRequest);
                    }
                }
            }
        }
    }

    private Page<Project> fetchResearcherProjects(String
                                                          email, Set<String> bridgeheads, Optional<ProjectState> projectState,
                                                  Optional<Boolean> archived, PageRequest pageRequest, boolean modifiedDescendant) {
        if (projectState.isEmpty()) {
            if (archived.isEmpty()) {
                if (modifiedDescendant) {
                    return projectRepository.findByEmailAndBridgeheadsModifiedAtDesc(email, bridgeheads, pageRequest);
                } else {
                    return projectRepository.findByEmailAndBridgeheadsModifiedAtAsc(email, bridgeheads, pageRequest);
                }
            } else {
                if (archived.get()) {
                    if (modifiedDescendant) {
                        return projectRepository.findArchivedProjectsByEmailAndBridgeheadsModifiedAtDesc(email, bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findArchivedProjectsByEmailAndBridgeheadsModifiedAtAsc(email, bridgeheads, pageRequest);
                    }
                } else {
                    if (modifiedDescendant) {
                        return projectRepository.findNotArchivedProjectsByEmailAndBridgeheadsModifiedAtDesc(email, bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findNotArchivedProjectsByEmailAndBridgeheadsModifiedAtAsc(email, bridgeheads, pageRequest);
                    }
                }
            }
        } else {
            if (archived.isEmpty()) {
                if (modifiedDescendant) {
                    return projectRepository.findByEmailAndStateAndBridgeheadsModifiedAtDesc(email, projectState.get(), bridgeheads, pageRequest);
                } else {
                    return projectRepository.findByEmailAndStateAndBridgeheadsModifiedAtAsc(email, projectState.get(), bridgeheads, pageRequest);
                }
            } else {
                if (archived.get()) {
                    if (modifiedDescendant) {
                        return projectRepository.findArchivedProjectsByEmailAndStateAndBridgeheadsModifiedAtDesc(email, projectState.get(), bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findArchivedProjectsByEmailAndStateAndBridgeheadsModifiedAtAsc(email, projectState.get(), bridgeheads, pageRequest);
                    }
                } else {
                    if (modifiedDescendant) {
                        return projectRepository.findNotArchivedProjectsByEmailAndStateAndBridgeheadsModifiedAtDesc(email, projectState.get(), bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findNotArchivedProjectsByEmailAndStateAndBridgeheadsModifiedAtAsc(email, projectState.get(), bridgeheads, pageRequest);
                    }
                }
            }
        }
    }

}
