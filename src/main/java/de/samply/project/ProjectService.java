package de.samply.project;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectRepository;
import de.samply.form.FormService;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.project.state.UserProjectState;
import de.samply.query.OutputFormat;
import de.samply.query.QueryService;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class ProjectService {


    private final SessionUser sessionUser;
    private final ProjectConfigurations projectConfigurations;

    // Services
    private final NotificationService notificationService;
    private final FormService formService;
    private final QueryService queryService;
    private final ProjectBridgeheadService projectBridgeheadService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;

    // Repositories
    private final ProjectRepository projectRepository;


    public ProjectService(NotificationService notificationService,
                          ProjectRepository projectRepository,
                          SessionUser sessionUser,
                          ProjectConfigurations projectConfigurations,
                          FormService formService,
                          QueryService queryService,
                          ProjectBridgeheadService projectBridgeheadService,
                          ProjectBridgeheadUserService projectBridgeheadUserService) {
        this.notificationService = notificationService;
        this.projectRepository = projectRepository;
        this.queryService = queryService;
        this.projectBridgeheadService = projectBridgeheadService;
        this.sessionUser = sessionUser;
        this.projectConfigurations = projectConfigurations;
        this.formService = formService;
        this.projectBridgeheadUserService = projectBridgeheadUserService;
    }

    public Project fetchProject(@NotNull String projectCode) throws ProjectServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new ProjectServiceException("ProjectCode " + projectCode + " not found");
        }
        return project.get();
    }

    public void saveProject(@NotNull Project project) {
        project.setModifiedAt(Instant.now());
        projectRepository.save(project);
    }

    public void updateBridgeheads(Project project, String[] bridgeheads) {
        Set<String> editionBridgeheads = Set.of(bridgeheads);

        // Remove bridgeheads that are no longer present
        projectBridgeheadService
                .fetchBridgeheads(project)
                .stream()
                .filter(projectBridgehead ->
                        !editionBridgeheads.contains(projectBridgehead.getBridgehead()))
                .forEach(projectBridgeheadService::deleteBridgehead);

        // Add new bridgeheads
        Set<String> oldBridgeheads = new HashSet<>(projectBridgeheadService
                .fetchBridgeheads(project)
                .stream()
                .map(ProjectBridgehead::getBridgehead)
                .toList());

        editionBridgeheads
                .stream()
                .filter(bridgehead -> !oldBridgeheads.contains(bridgehead))
                .forEach(bridgehead -> createProjectBridgehead(project, bridgehead));

        this.notificationService.createNotification(project, null, sessionUser.getEmail(),
                OperationType.EDIT_PROJECT, "Changed bridgeheads: " + String.join("," + Arrays.toString(bridgeheads)), null, null);

    }

    private void createProjectBridgehead(Project project, String bridgehead) {
        ProjectBridgehead projectBridgehead = new ProjectBridgehead();
        projectBridgehead.setBridgehead(bridgehead);
        projectBridgehead.setProject(project);
        projectBridgehead.setState(ProjectBridgeheadState.CREATED);
        projectBridgeheadService.saveBridgehead(projectBridgehead);
    }

    public List<Project> fetchAllUserVisibleProjects() {
        // Fetch projects as a project manager
        if (isProjectManagerAdmin()) {
            return projectRepository.findAll();
        }
        Set<String> bridgeheads = sessionUser.getBridgeheads();
        // Fetch projects as bridgehead admin
        // We make an assumption: A bridgehead admin is bridgehead admin in all of their bridgeheads.
        if (isBridgeheadAdmin()) {
            return projectRepository.findByBridgeheadsOrCreator(sessionUser.getEmail(), bridgeheads);
        }
        // Fetch projects as a researcher
        return projectBridgeheadUserService.fetchProjects(sessionUser.getEmail());
    }

    protected boolean isProjectManagerAdmin() {
        return sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.PROJECT_MANAGER_ADMIN);
    }

    protected boolean isBridgeheadAdmin() {
        for (String bridgehead : sessionUser.getBridgeheads()) {
            if (sessionUser.getUserOrganisationRoles()
                    .getBridgeheadRoles(bridgehead).contains(OrganisationRole.BRIDGEHEAD_ADMIN)) {
                return true;
            }
        }
        return false;
    }

    protected Page<Project> fetchProjectManagerAdminProjects(
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

    protected Page<Project> fetchBridgeheadAdminProjects(
            Set<String> bridgeheads, Optional<ProjectState> projectState, Optional<Boolean> archived,
            PageRequest pageRequest, boolean modifiedDescendant) {
        if (projectState.isEmpty()) {
            if (archived.isEmpty()) {
                if (modifiedDescendant) {
                    return projectRepository.findByBridgeheadsOrCreatorModifiedAtDesc(sessionUser.getEmail(), bridgeheads, pageRequest);
                } else {
                    return projectRepository.findByBridgeheadsOrCreatorModifiedAtAsc(sessionUser.getEmail(), bridgeheads, pageRequest);
                }
            } else {
                if (archived.get()) {
                    if (modifiedDescendant) {
                        return projectRepository.findArchivedProjectsByBridgeheadsOrCreatorModifiedAtDesc(sessionUser.getEmail(), bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findArchivedProjectsByBridgeheadsOrCreatorModifiedAtAsc(sessionUser.getEmail(), bridgeheads, pageRequest);
                    }
                } else {
                    if (modifiedDescendant) {
                        return projectRepository.findNotArchivedProjectsByBridgeheadsOrCreatorModifiedAtDesc(sessionUser.getEmail(), bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findNotArchivedProjectsByBridgeheadsOrCreatorModifiedAtAsc(sessionUser.getEmail(), bridgeheads, pageRequest);
                    }
                }
            }
        } else {
            if (archived.isEmpty()) {
                if (modifiedDescendant) {
                    return projectRepository.findByStateAndBridgeheadsOrCreatorModifiedAtDesc(sessionUser.getEmail(), projectState.get(), bridgeheads, pageRequest);
                } else {
                    return projectRepository.findByStateAndBridgeheadsOrCreatorModifiedAtAsc(sessionUser.getEmail(), projectState.get(), bridgeheads, pageRequest);
                }
            } else {
                if (archived.get()) {
                    if (modifiedDescendant) {
                        return projectRepository.findArchivedProjectsByStateAndBridgeheadsOrCreatorModifiedAtDesc(sessionUser.getEmail(), projectState.get(), bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findArchivedProjectsByStateAndBridgeheadsOrCreatorModifiedAtAsc(sessionUser.getEmail(), projectState.get(), bridgeheads, pageRequest);
                    }
                } else {
                    if (modifiedDescendant) {
                        return projectRepository.findNotArchivedProjectsByStateAndBridgeheadsOrCreatorModifiedAtDesc(sessionUser.getEmail(), projectState.get(), bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findNotArchivedProjectsByStateAndBridgeheadsOrCreatorModifiedAtAsc(sessionUser.getEmail(), projectState.get(), bridgeheads, pageRequest);
                    }
                }
            }
        }
    }

    protected Page<Project> fetchResearcherProjects(String
                                                            email, Set<String> bridgeheads, Optional<ProjectState> projectState,
                                                    Optional<Boolean> archived, PageRequest pageRequest, boolean modifiedDescendant) {
        if (projectState.isEmpty()) {
            if (archived.isEmpty()) {
                if (modifiedDescendant) {
                    return projectRepository.findByEmailAndBridgeheadsOrCreatorModifiedAtDesc(email, bridgeheads, pageRequest);
                } else {
                    return projectRepository.findByEmailAndBridgeheadsOrCreatorModifiedAtAsc(email, bridgeheads, pageRequest);
                }
            } else {
                if (archived.get()) {
                    if (modifiedDescendant) {
                        return projectRepository.findArchivedProjectsByEmailAndBridgeheadsOrCreatorModifiedAtDesc(email, bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findArchivedProjectsByEmailAndBridgeheadsOrCreatorModifiedAtAsc(email, bridgeheads, pageRequest);
                    }
                } else {
                    if (modifiedDescendant) {
                        return projectRepository.findNotArchivedProjectsByEmailAndBridgeheadsOrCreatorModifiedAtDesc(email, bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findNotArchivedProjectsByEmailAndBridgeheadsOrCreatorModifiedAtAsc(email, bridgeheads, pageRequest);
                    }
                }
            }
        } else {
            if (archived.isEmpty()) {
                if (modifiedDescendant) {
                    return projectRepository.findByEmailAndStateAndBridgeheadsOrCreatorModifiedAtDesc(email, projectState.get(), bridgeheads, pageRequest);
                } else {
                    return projectRepository.findByEmailAndStateAndBridgeheadsOrCreatorModifiedAtAsc(email, projectState.get(), bridgeheads, pageRequest);
                }
            } else {
                if (archived.get()) {
                    if (modifiedDescendant) {
                        return projectRepository.findArchivedProjectsByEmailAndStateAndBridgeheadsOrCreatorModifiedAtDesc(email, projectState.get(), bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findArchivedProjectsByEmailAndStateAndBridgeheadsOrCreatorModifiedAtAsc(email, projectState.get(), bridgeheads, pageRequest);
                    }
                } else {
                    if (modifiedDescendant) {
                        return projectRepository.findNotArchivedProjectsByEmailAndStateAndBridgeheadsOrCreatorModifiedAtDesc(email, projectState.get(), bridgeheads, pageRequest);
                    } else {
                        return projectRepository.findNotArchivedProjectsByEmailAndStateAndBridgeheadsOrCreatorModifiedAtAsc(email, projectState.get(), bridgeheads, pageRequest);
                    }
                }
            }
        }
    }

    public Map<ProjectType, List<OutputFormat>> fetchOutputFormats(@NotNull Project project) throws ProjectServiceException {
        Map<ProjectType, List<OutputFormat>> result = new HashMap<>();
        project.fetchProjectTypes().forEach(projectType ->
                result.put(projectType, fetchOutputFormats(projectType)));
        return result;
    }

    private List<OutputFormat> fetchOutputFormats(ProjectType projectType) {
        return (projectType == ProjectType.DATASHIELD) ?
                List.of(OutputFormat.OPAL) :
                Arrays.stream(OutputFormat.values()).filter(outputFormat -> outputFormat != OutputFormat.OPAL).toList();
    }

    public void setProjectConfiguration(@NotNull Project project, @NotNull String projectConfigurationName) throws ProjectServiceException {
        if (!projectConfigurationName.equals(ProjectManagerConst.CUSTOM_PROJECT_CONFIGURATION)) {
            de.samply.frontend.dto.ProjectAndForms projectAndForms = this.projectConfigurations.getConfig().get(projectConfigurationName);
            if (projectAndForms == null) {
                throw new ProjectServiceException("ProjectCode configuration " + projectConfigurationName + " not found");
            }

            Project mergedProject = DtoFactory.merge(projectAndForms.project(), project);
            mergedProject.setIsCustomConfigSelected(false);
            saveProject(mergedProject);
            queryService.saveQuery(mergedProject.getQuery());

            // Synchronize Forms
            if (projectAndForms.forms() != null && projectAndForms.forms().length > 0) {
                this.formService.syncSelectedForms(project, Arrays.stream(projectAndForms.forms()).map(Form::title).toList());
            }

            // Synchronize Form Fields
            this.formService.editProjectFormFieldValues(Optional.ofNullable(projectAndForms.formFields()), project);

        } else if (!Boolean.TRUE.equals(project.getIsCustomConfigSelected())) {
            project.setIsCustomConfigSelected(true);
            saveProject(project);
        }
    }

    public void addProjectResultUrl(@NotNull Project project, @NotNull String resultUrl) throws ProjectServiceException {
        project.setResultsUrl(resultUrl);
        project.setCreatorResultsState(UserProjectState.CREATED); // The creator should accept the new results again
        saveProject(project);
    }

    public void acceptResultsByCreator(@NotNull Project project) throws ProjectServiceException {
        changeCreatorResultsState(project, UserProjectState.ACCEPTED);
    }

    public void rejectResultsForCreator(@NotNull Project project) throws ProjectServiceException {
        changeCreatorResultsState(project, UserProjectState.REJECTED);
    }

    public void requestChangesInResultsForCreator(@NotNull Project project) throws ProjectServiceException {
        changeCreatorResultsState(project, UserProjectState.REQUEST_CHANGES);
    }

    private void changeCreatorResultsState(@NotNull Project project, UserProjectState state) throws ProjectServiceException {
        project.setCreatorResultsState(state);
        saveProject(project);
    }

    public List<Project> findProjectByExpiresAtBeforeAndStateIn(LocalDate expirationTime, Set<ProjectState> states) {
        return projectRepository.findByExpiresAtBeforeAndStateIn(expirationTime, states);
    }

}
