package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadDataShield;
import de.samply.db.repository.ProjectBridgeheadDataShieldRepository;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.project.state.UserProjectState;
import de.samply.query.QueryState;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProjectBridgeheadService {

    // Services
    private final NotificationService notificationService;

    // Repositories
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository;

    private final SessionUser sessionUser;

    public ProjectBridgeheadService(NotificationService notificationService,
                                    ProjectBridgeheadRepository projectBridgeheadRepository,
                                    ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                                    ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository,
                                    SessionUser sessionUser) {
        this.notificationService = notificationService;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.projectBridgeheadDataShieldRepository = projectBridgeheadDataShieldRepository;
        this.sessionUser = sessionUser;
    }

    public void acceptProject(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) throws ProjectBridgeheadServiceException {
        changeProjectBridgeheadState(project, bridgehead, ProjectBridgeheadState.ACCEPTED);
    }

    public void rejectProject(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) throws ProjectBridgeheadServiceException {
        changeProjectBridgeheadState(project, bridgehead, ProjectBridgeheadState.REJECTED);
    }

    private void changeProjectBridgeheadState(@NotNull Project project, @NotNull ProjectBridgehead bridgehead, @NotNull ProjectBridgeheadState state) throws ProjectBridgeheadServiceException {
        bridgehead.setState(state);
        setModifiedAtAndSaveBridgehead(bridgehead);
        this.notificationService.createNotification(project, bridgehead.getBridgehead(), sessionUser.getEmail(), OperationType.CHANGE_PROJECT_STATE,
                "Set project bridgehead state to " + state, null, null);
    }

    public Optional<ProjectBridgehead> fetchProjectBridgehead(String projectCode, String bridgehead) throws ProjectBridgeheadServiceException {
        return projectBridgeheadRepository.findFirstByBridgeheadAndProject_Code(bridgehead, projectCode);
    }

    protected boolean isProjectManagerAdmin() {
        return sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.PROJECT_MANAGER_ADMIN);
    }

    protected boolean isBridgeheadAdminOfProjectBridgehead(ProjectBridgehead projectBridgehead) {
        return sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.BRIDGEHEAD_ADMIN, Optional.of(projectBridgehead.getBridgehead()));
    }

    protected boolean isUserOfProjectBridgehead(ProjectBridgehead projectBridgehead) {
        if (projectBridgehead.getProject().getCreatorEmail().equals(sessionUser.getEmail())) {
            for (String bridgehead : sessionUser.getBridgeheads()) {
                if (projectBridgehead.getBridgehead().equals(bridgehead)) {
                    return true;
                }
            }
        }
        return !projectBridgeheadUserRepository.getByEmailAndProjectBridgehead(sessionUser.getEmail(), projectBridgehead).isEmpty();
    }

    protected boolean isUserCreatorOfProject(Project project) {
        return project.getCreatorEmail().equals(sessionUser.getEmail());
    }

    public void scheduleSendQueryToBridgehead(@NotNull ProjectBridgehead bridgehead, @NotNull ProjectType projectType) throws ProjectBridgeheadServiceException {
        changeQueryState(bridgehead, QueryState.TO_BE_SENT, projectType);
    }

    public void scheduleSendQueryToBridgeheadAndExecute(@NotNull ProjectBridgehead bridgehead, @NotNull ProjectType projectType) throws ProjectBridgeheadServiceException {
        changeQueryState(bridgehead, QueryState.TO_BE_SENT_AND_EXECUTED, projectType);
    }

    private void changeQueryState(ProjectBridgehead bridgehead, QueryState queryState, ProjectType projectType) throws ProjectBridgeheadServiceException {
        bridgehead.addOrUpdateExecution(projectType, queryState, null, sessionUser.getEmail(), null, null);
        setModifiedAtAndSaveBridgehead(bridgehead);
    }

    public void addResultsUrl(@NotNull ProjectBridgehead bridgehead, @NotNull String resultsUrl) throws ProjectBridgeheadServiceException {
        bridgehead.setResultsUrl(resultsUrl);
        bridgehead.setCreatorResultsState(UserProjectState.CREATED); // The creator should accept the results again
        setModifiedAtAndSaveBridgehead(bridgehead);
    }

    public void acceptResultsForCreator(@NotNull ProjectBridgehead bridgehead) throws ProjectBridgeheadServiceException {
        changeCreatorResultsState(bridgehead, UserProjectState.ACCEPTED);
    }

    public void rejectResultsForCreator(@NotNull ProjectBridgehead bridgehead) throws ProjectBridgeheadServiceException {
        changeCreatorResultsState(bridgehead, UserProjectState.REJECTED);
    }

    public void requestChangesInResultsForCreator(@NotNull ProjectBridgehead bridgehead) throws ProjectBridgeheadServiceException {
        changeCreatorResultsState(bridgehead, UserProjectState.REQUEST_CHANGES);
    }

    private void changeCreatorResultsState(@NotNull ProjectBridgehead bridgehead, @NotNull UserProjectState state) throws ProjectBridgeheadServiceException {
        bridgehead.setCreatorResultsState(state);
        setModifiedAtAndSaveBridgehead(bridgehead);
    }

    public void saveBridgehead(@NotNull ProjectBridgehead bridgehead) throws ProjectBridgeheadServiceException {
        addAllExecutions(bridgehead);
        setModifiedAtAndSaveBridgehead(bridgehead);
    }

    private void setModifiedAtAndSaveBridgehead(@NotNull ProjectBridgehead bridgehead) throws ProjectBridgeheadServiceException {
        bridgehead.setModifiedAt(Instant.now());
        projectBridgeheadRepository.save(bridgehead);
    }

    public Set<ProjectBridgehead> fetchBridgeheads(Project project) {
        return projectBridgeheadRepository.findByProject(project);
    }

    public Set<ProjectBridgehead> fetchBridgeheads(Project project, ProjectBridgeheadState projectBridgeheadState) {
        return projectBridgeheadRepository.findByProjectAndState(project, projectBridgeheadState);
    }

    public Optional<ProjectBridgehead> fetchBridgehead(Project project, String bridgehead) {
        return projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project);
    }

    public Optional<ProjectBridgeheadDataShield> fetchDataShield(ProjectBridgehead bridgehead) {
        return projectBridgeheadDataShieldRepository.findByProjectBridgehead(bridgehead);
    }

    public void saveDataShield(ProjectBridgeheadDataShield dataShield) {
        projectBridgeheadDataShieldRepository.save(dataShield);
    }

    public Set<ProjectBridgehead> fetchBridgeheads(QueryState queryState, Set<ProjectState> projectStates) {
        return projectBridgeheadRepository.getByQueryStateAndProjectState(queryState, projectStates);
    }

    public void deleteBridgehead(ProjectBridgehead bridgehead) {
        projectBridgeheadRepository.delete(bridgehead);
    }

    public List<ProjectBridgehead> fetchByProjectTypeAndNotProjectState(ProjectType projectType, Set<ProjectState> projectStates) {
        return projectBridgeheadRepository.getByProjectTypeAndNotProjectState(projectType, projectStates);
    }

    private void addAllExecutions(ProjectBridgehead bridgehead) {
        bridgehead
                .getProject()
                .getQuery()
                .fetchProjectTypes()
                .forEach(projectType -> addOrUpdateExecution(bridgehead, projectType));
    }

    private void addOrUpdateExecution(ProjectBridgehead bridgehead, ProjectType projectType) {
        bridgehead.addOrUpdateExecution(
                projectType,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * REQUIRES_NEW is mandatory because this method is called after the original transaction
     * has already committed — there is no active transaction to join, so Spring must open a
     * fresh one to allow JPA dirty-checking and orphanRemoval to work correctly.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleChange(Long queryId) {
        projectBridgeheadRepository
                .findByProject_Query_Id(queryId)
                .forEach(this::updateQueryInBridgehead);
    }

    private void updateQueryInBridgehead(ProjectBridgehead bridgehead) {
        Set<ProjectType> expectedTypes = bridgehead.getProject().fetchProjectTypes();

        // ADD missing executions
        expectedTypes.forEach(type -> addOrUpdateExecution(bridgehead, type));
        // REMOVE obsolete executions
        bridgehead.getExecutions().removeIf(exec ->
                !expectedTypes.contains(
                        exec.getQueryOutput().getProjectType()
                )
        );
        bridgehead.setModifiedAt(Instant.now());
    }


}
