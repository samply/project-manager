package de.samply.project;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.db.repository.QueryRepository;
import de.samply.form.FormService;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.Results;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.project.state.UserProjectState;
import de.samply.query.OutputFormat;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ProjectService {

    private final NotificationService notificationService;
    private final ProjectRepository projectRepository;
    private final QueryRepository queryRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final SessionUser sessionUser;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final ProjectConfigurations projectConfigurations;
    private final DtoFactory dtoFactory;
    private final FormService formService;

    public ProjectService(NotificationService notificationService,
                          ProjectRepository projectRepository,
                          QueryRepository queryRepository,
                          ProjectBridgeheadRepository projectBridgeheadRepository,
                          SessionUser sessionUser,
                          ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                          ProjectConfigurations projectConfigurations,
                          DtoFactory dtoFactory,
                          FormService formService) {
        this.notificationService = notificationService;
        this.projectRepository = projectRepository;
        this.queryRepository = queryRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.sessionUser = sessionUser;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.projectConfigurations = projectConfigurations;
        this.dtoFactory = dtoFactory;
        this.formService = formService;
    }

    public de.samply.frontend.dto.Project fetchProject(@NotNull String projectCode) throws ProjectServiceException {
        Optional<Project> projectOptional = this.projectRepository.findByCode(projectCode);
        if (projectOptional.isEmpty()) {
            throw new ProjectServiceException("Project " + projectCode + " not found");
        }
        return dtoFactory.convert(projectOptional.get());
    }


    private void saveProject(@NotNull Project project) {
        project.setModifiedAt(Instant.now());
        projectRepository.save(project);
    }

    public void updateBridgeheads(String projectCode, String[] bridgeheads) {
        this.projectRepository.findByCode(projectCode).ifPresent(project -> {
            Set<String> editionBridgeheads = Set.of(bridgeheads);
            // Remove bridgeheads that are no longer present
            projectBridgeheadRepository.findByProject(project).stream().filter(projectBridgehead ->
                    !editionBridgeheads.contains(projectBridgehead.getBridgehead())).forEach(projectBridgeheadRepository::delete);
            // Add new bridgeheads
            Set<String> oldBridgeheads = new HashSet<>(projectBridgeheadRepository.findByProject(project).stream().
                    map(ProjectBridgehead::getBridgehead).toList());
            editionBridgeheads.stream().filter(bridgehead -> !oldBridgeheads.contains(bridgehead)).forEach(bridgehead ->
                    createProjectBridgehead(project, bridgehead));
            this.notificationService.createNotification(project.getCode(), null, sessionUser.getEmail(),
                    OperationType.EDIT_PROJECT, "Changed bridgeheads: " + String.join("," + Arrays.toString(bridgeheads)), null, null);

        });
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
        return projectBridgeheadUserRepository.findProjectsByEmail(sessionUser.getEmail());
    }

    public Page<de.samply.frontend.dto.Project> fetchUserVisibleProjects(
            Optional<ProjectState> projectState, Optional<Boolean> archived, int page, int pageSize,
            boolean modifiedDescendant) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        if (isProjectManagerAdmin()) {
            return fetchProjectManagerAdminProjects(projectState, archived, pageRequest, modifiedDescendant).map(dtoFactory::convert);
        }
        Set<String> bridgeheads = sessionUser.getBridgeheads();
        // We make an assumption: A bridgehead admin is bridgehead admin in all of their bridgeheads.
        if (isBridgeheadAdmin()) {
            return fetchBridgeheadAdminProjects(bridgeheads, projectState, archived, pageRequest, modifiedDescendant).map(dtoFactory::convert);
        }
        return fetchResearcherProjects(sessionUser.getEmail(), bridgeheads, projectState, archived, pageRequest, modifiedDescendant).map(dtoFactory::convert);
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

    private Page<Project> fetchResearcherProjects(String
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

    public Map<ProjectType, List<OutputFormat>> fetchOutputFormats(@NotNull String projectCode) throws ProjectServiceException {
        Optional<Project> projectOptional = this.projectRepository.findByCode(projectCode);
        if (projectOptional.isEmpty()) {
            throw new ProjectServiceException("Project " + projectCode + " not found");
        }
        Map<ProjectType, List<OutputFormat>> result = new HashMap<>();
        projectOptional.get().fetchProjectTypes().forEach(projectType ->
                result.put(projectType, fetchOutputFormats(projectType)));
        return result;
    }

    public void removeOutput(@NotNull String projectCode, @NotNull ProjectType projectType) {
        this.projectRepository.findByCode(projectCode).ifPresentOrElse(project -> {
                    project.removeOutput(projectType);
                    project.setModifiedAt(Instant.now());
                    saveProject(project);
                    this.notificationService.createNotification(project.getCode(), null, sessionUser.getEmail(),
                            OperationType.EDIT_PROJECT, "Removed output format " + projectType.name(), null, null);
                },
                () -> {
                    throw new ProjectServiceException("Project " + projectCode + " not found");
                }
        );
    }

    private List<OutputFormat> fetchOutputFormats(ProjectType projectType) {
        return (projectType == ProjectType.DATASHIELD) ?
                List.of(OutputFormat.OPAL) :
                Arrays.stream(OutputFormat.values()).filter(outputFormat -> outputFormat != OutputFormat.OPAL).toList();
    }

    public Map<String, de.samply.frontend.dto.ProjectAndForms> fetchCurrentProjectConfiguration(@NotNull String projectCode) throws ProjectServiceException {
        Optional<Project> projectOptional = this.projectRepository.findByCode(projectCode);
        if (projectOptional.isEmpty()) {
            throw new ProjectServiceException("Project " + projectCode + " not found");
        }
        return this.projectConfigurations.fetchCurrentProjectConfiguration(dtoFactory.convertToProjectAndForms(projectOptional.get(), Optional.empty()));
    }

    public void setProjectConfiguration(@NotNull String projectCode, @NotNull String projectConfigurationName) throws ProjectServiceException {
        Optional<Project> projectOptional = this.projectRepository.findByCode(projectCode);
        if (projectOptional.isEmpty()) {
            throw new ProjectServiceException("Project " + projectCode + " not found");
        }
        if (!projectConfigurationName.equals(ProjectManagerConst.CUSTOM_PROJECT_CONFIGURATION)) {
            de.samply.frontend.dto.ProjectAndForms projectAndForms = this.projectConfigurations.getConfig().get(projectConfigurationName);
            if (projectAndForms == null) {
                throw new ProjectServiceException("Project configuration " + projectConfigurationName + " not found");
            }

            // Synchronize Project
            Project project = DtoFactory.merge(projectAndForms.project(), projectOptional.get());
            project.setCustomConfig(false);
            saveProject(project);
            this.queryRepository.save(project.getQuery());

            // Synchronize Forms
            if (projectAndForms.forms() != null && projectAndForms.forms().length > 0) {
                this.formService.syncSelectedForms(projectCode, Arrays.stream(projectAndForms.forms()).map(Form::title).toList());
            }

            // Synchronize Form Fields
            this.formService.editProjectFormFieldValues(Optional.ofNullable(projectAndForms.formFields()), projectCode);

        } else if (!projectOptional.get().isCustomConfig()) {
            projectOptional.get().setCustomConfig(true);
            saveProject(projectOptional.get());
        }
    }

    public void addProjectResultUrl(@NotNull String projectCode, @NotNull String resultUrl) throws ProjectServiceException {
        Optional<Project> project = this.projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new ProjectServiceException("Project " + projectCode + " not found");
        }
        project.get().setResultsUrl(resultUrl);
        project.get().setCreatorResultsState(UserProjectState.CREATED); // The creator should accept the new results again
        saveProject(project.get());
    }

    public void acceptResultsByCreator(@NotNull String projectCode) throws ProjectServiceException {
        changeCreatorResultsState(projectCode, UserProjectState.ACCEPTED);
    }

    public void rejectResultsForCreator(@NotNull String projectCode) throws ProjectServiceException {
        changeCreatorResultsState(projectCode, UserProjectState.REJECTED);
    }

    public void requestChangesInResultsForCreator(@NotNull String projectCode) throws ProjectServiceException {
        changeCreatorResultsState(projectCode, UserProjectState.REQUEST_CHANGES);
    }

    private void changeCreatorResultsState(@NotNull String projectCode, UserProjectState state) throws ProjectServiceException {
        Optional<Project> project = this.projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new ProjectServiceException("Project " + projectCode + " not found");
        }
        project.get().setCreatorResultsState(state);
        saveProject(project.get());
    }

    public Optional<Results> fetchResults(@NotNull String projectCode) throws ProjectServiceException {
        Optional<Project> project = this.projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new ProjectServiceException("Project " + projectCode + " not found");
        }
        return dtoFactory.fetchResults(project.get());
    }

}
