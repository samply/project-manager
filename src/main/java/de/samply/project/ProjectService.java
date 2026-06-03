package de.samply.project;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.*;
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
import de.samply.query.QueryPersistenceService;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import jakarta.persistence.criteria.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final QueryPersistenceService queryPersistenceService;
    private final ProjectBridgeheadService projectBridgeheadService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;

    // Repositories
    private final ProjectRepository projectRepository;


    public ProjectService(NotificationService notificationService,
                          ProjectRepository projectRepository,
                          SessionUser sessionUser,
                          ProjectConfigurations projectConfigurations,
                          FormService formService,
                          QueryPersistenceService queryPersistenceService,
                          ProjectBridgeheadService projectBridgeheadService,
                          ProjectBridgeheadUserService projectBridgeheadUserService) {
        this.notificationService = notificationService;
        this.projectRepository = projectRepository;
        this.queryPersistenceService = queryPersistenceService;
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

    @Transactional
    public void deleteProject(@NotNull Project project) {
        Long queryId = Optional.ofNullable(project.getQuery())
                .map(Query::getId)
                .orElse(null);
        projectRepository.delete(project);
        projectRepository.flush();
        if (queryId != null) {
            queryPersistenceService.deleteQueryIfOrphan(queryId);
        }
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

    public ProjectState[] fetchVisibleProjectStates() {
        boolean projectManagerAdmin = isProjectManagerAdmin();
        return fetchAllUserVisibleProjects().stream()
                .map(Project::getState)
                .filter(Objects::nonNull)
                .filter(state -> !projectManagerAdmin || state != ProjectState.DRAFT)
                .distinct()
                .toArray(ProjectState[]::new);
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

    /**
     * Fetches one page of projects that satisfy the requested filters and are visible to the
     * current user. Filtering and authorization are executed in the database as one query.
     */
    protected Page<Project> fetchUserVisibleProjects(
            Optional<ProjectState> projectState, Optional<Boolean> archived, PageRequest pageRequest,
            Optional<String> projectCreator, Optional<String> bridgehead) {
        Specification<Project> specification =
                buildUserVisibleProjectsSpecification(projectState, archived, projectCreator, bridgehead);
        return projectRepository.findAll(specification, pageRequest);
    }

    /**
     * Combines the independent state, archive, and user-visibility rules into one specification.
     */
    private Specification<Project> buildUserVisibleProjectsSpecification(
            Optional<ProjectState> projectState, Optional<Boolean> archived,
            Optional<String> projectCreator, Optional<String> bridgehead) {
        boolean isProjectManagerAdmin = isProjectManagerAdmin();
        // allOf combines the independent specifications with a logical AND.
        return Specification.allOf(List.of(
                buildProjectStateSpecification(projectState, isProjectManagerAdmin),
                buildArchivedStatusSpecification(archived),
                buildUserVisibilitySpecification(isProjectManagerAdmin),
                buildProjectCreatorSpecification(projectCreator),
                buildBridgeheadSpecification(bridgehead)
        ));
    }

    private Specification<Project> buildProjectCreatorSpecification(Optional<String> projectCreator) {
        return (project, query, criteriaBuilder) -> projectCreator
                .filter(StringUtils::hasText)
                .map(filter -> {
                    String pattern = "%" + filter.toLowerCase(Locale.ROOT) + "%";
                    Predicate emailMatch = criteriaBuilder.like(
                            criteriaBuilder.lower(project.get(Project_.creatorEmail)), pattern);

                    Subquery<Long> userQuery = query.subquery(Long.class);
                    Root<User> user = userQuery.from(User.class);
                    Predicate sameEmail = criteriaBuilder.equal(
                            criteriaBuilder.lower(user.get(User_.email)),
                            criteriaBuilder.lower(project.get(Project_.creatorEmail)));
                    Predicate nameMatch = criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(user.get(User_.firstName)), pattern),
                            criteriaBuilder.like(criteriaBuilder.lower(user.get(User_.lastName)), pattern));
                    return criteriaBuilder.or(emailMatch,
                            criteriaBuilder.exists(userQuery.select(user.get(User_.id)).where(sameEmail, nameMatch)));
                })
                .orElseGet(criteriaBuilder::conjunction);
    }

    private Specification<Project> buildBridgeheadSpecification(Optional<String> bridgehead) {
        return (project, query, criteriaBuilder) -> bridgehead
                .filter(StringUtils::hasText)
                .map(filter -> {
                    String pattern = "%" + filter.toLowerCase(Locale.ROOT) + "%";
                    Subquery<Long> bridgeheadQuery = query.subquery(Long.class);
                    Root<ProjectBridgehead> projectBridgehead = bridgeheadQuery.from(ProjectBridgehead.class);
                    return criteriaBuilder.exists(bridgeheadQuery.select(projectBridgehead.get(ProjectBridgehead_.id)).where(
                            criteriaBuilder.equal(projectBridgehead.get(ProjectBridgehead_.project), project),
                            criteriaBuilder.like(criteriaBuilder.lower(
                                    projectBridgehead.get(ProjectBridgehead_.bridgehead)), pattern)));
                })
                .orElseGet(criteriaBuilder::conjunction);
    }

    /**
     * Filters by the requested state. Without an explicit state, project manager admins see every
     * state except DRAFT; other users receive no state restriction here because their visibility
     * is handled separately.
     */
    private Specification<Project> buildProjectStateSpecification(
            Optional<ProjectState> projectState, boolean isProjectManagerAdmin) {
        // Criterion: match the requested state or exclude DRAFT by default for admins.
        return (project, _, criteriaBuilder) -> projectState
                .map(state ->
                        criteriaBuilder.equal(project.get(Project_.state), state))
                .orElseGet(() -> isProjectManagerAdmin
                        ? criteriaBuilder.notEqual(project.get(Project_.state), ProjectState.DRAFT)
                        : criteriaBuilder.conjunction());
    }

    /**
     * Interprets archived=true as having an archive timestamp and archived=false as not having
     * one. An omitted parameter does not restrict the archive status.
     */
    private Specification<Project> buildArchivedStatusSpecification(Optional<Boolean> archived) {
        // Criterion: archivedAt must be present for archived projects and absent otherwise.
        return (project, _, criteriaBuilder) -> archived
                .map(isArchived -> isArchived
                        ? criteriaBuilder.isNotNull(project.get(Project_.archivedAt))
                        : criteriaBuilder.isNull(project.get(Project_.archivedAt)))
                .orElseGet(criteriaBuilder::conjunction);
    }

    /**
     * Project manager admins have unrestricted visibility. Other users can see projects they
     * created or projects reachable through one of their bridgeheads.
     */
    private Specification<Project> buildUserVisibilitySpecification(boolean isProjectManagerAdmin) {
        if (isProjectManagerAdmin) {
            // A conjunction adds no visibility restriction for project manager admins.
            return (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();
        }

        String email = sessionUser.getEmail();
        Set<String> bridgeheads = sessionUser.getBridgeheads();
        boolean bridgeheadAdmin = isBridgeheadAdmin();

        // Criterion: the user is the creator OR a permitted bridgehead row exists.
        return (project, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.equal(project.get(Project_.creatorEmail), email),
                criteriaBuilder.exists(buildVisibleBridgeheadSubquery(
                        project, query, criteriaBuilder, email, bridgeheads, bridgeheadAdmin))
        );
    }

    /**
     * Builds the correlated bridgehead lookup used by the outer project query. Bridgehead admins
     * only need a matching bridgehead assignment. Researchers additionally need a matching
     * ProjectBridgeheadUser entry for their email address.
     */
    private Subquery<Integer> buildVisibleBridgeheadSubquery(
            Root<Project> project,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder,
            String email,
            Set<String> bridgeheads,
            boolean bridgeheadAdmin) {
        Subquery<Integer> bridgeheadQuery = query.subquery(Integer.class);
        Root<ProjectBridgehead> projectBridgehead = bridgeheadQuery.from(ProjectBridgehead.class);
        // Criteria: correlate the outer project and restrict it to the user's bridgeheads.
        List<Predicate> predicates = buildBridgeheadVisibilityPredicates(
                project, projectBridgehead, criteriaBuilder, bridgeheads);
        if (!bridgeheadAdmin) {
            // Researchers additionally need an explicit assignment to that project bridgehead.
            predicates.addAll(buildResearcherVisibilityPredicates(
                    bridgeheadQuery, projectBridgehead, criteriaBuilder, email));
        }
        // EXISTS only needs a matching row, so select constant 1 and apply the visibility criteria.
        return bridgeheadQuery.select(criteriaBuilder.literal(1))
                .where(predicates.toArray(Predicate[]::new));
    }

    /**
     * Correlates the bridgehead record with the outer project and restricts it to bridgeheads
     * assigned to the current user.
     */
    private List<Predicate> buildBridgeheadVisibilityPredicates(
            Root<Project> project,
            Root<ProjectBridgehead> projectBridgehead,
            CriteriaBuilder criteriaBuilder,
            Set<String> bridgeheads) {
        return new ArrayList<>(List.of(
                criteriaBuilder.equal(projectBridgehead.get(ProjectBridgehead_.project), project),
                buildAssignedBridgeheadPredicate(
                        projectBridgehead, criteriaBuilder, bridgeheads)
        ));
    }

    /**
     * Returns a predicate that can match only the current user's bridgeheads. An empty set becomes
     * an always-false predicate, leaving project ownership as the user's only visibility path.
     */
    private Predicate buildAssignedBridgeheadPredicate(
            Root<ProjectBridgehead> projectBridgehead,
            CriteriaBuilder criteriaBuilder,
            Set<String> bridgeheads) {
        // An always-false predicate avoids an invalid empty IN clause.
        return bridgeheads.isEmpty()
                ? criteriaBuilder.disjunction()
                : projectBridgehead.get(ProjectBridgehead_.bridgehead).in(bridgeheads);
    }

    /**
     * Restricts a bridgehead match to a researcher explicitly assigned to that project bridgehead.
     */
    private List<Predicate> buildResearcherVisibilityPredicates(
            Subquery<Integer> bridgeheadQuery,
            Root<ProjectBridgehead> projectBridgehead,
            CriteriaBuilder criteriaBuilder,
            String email) {
        Root<ProjectBridgeheadUser> projectBridgeheadUser =
                bridgeheadQuery.from(ProjectBridgeheadUser.class);
        // Criteria: the assignment belongs to this bridgehead and to the current researcher.
        return List.of(
                criteriaBuilder.equal(
                        projectBridgeheadUser.get(ProjectBridgeheadUser_.projectBridgehead),
                        projectBridgehead),
                criteriaBuilder.equal(
                        projectBridgeheadUser.get(ProjectBridgeheadUser_.email), email)
        );
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
        final List<String> selectedConfigurations;
        try {
            selectedConfigurations = projectConfigurations.parseSelection(projectConfigurationName);
        } catch (IllegalArgumentException exception) {
            throw new ProjectServiceException(exception.getMessage());
        }

        if (!selectedConfigurations.contains(ProjectManagerConst.CUSTOM_PROJECT_CONFIGURATION)) {
            de.samply.frontend.dto.ProjectAndForms projectAndForms =
                    projectConfigurations.merge(selectedConfigurations);

            Project mergedProject = DtoFactory.merge(projectAndForms.project(), project);
            mergedProject.setIsCustomConfigSelected(false);
            saveProject(mergedProject);
            queryPersistenceService.saveQuery(mergedProject.getQuery());

            // Synchronize Forms
            if (projectAndForms.forms() != null) {
                this.formService.syncSelectedForms(
                        project,
                        Arrays.stream(projectAndForms.forms()).map(Form::title).toList());
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
