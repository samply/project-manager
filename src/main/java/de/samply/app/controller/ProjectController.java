package de.samply.app.controller;

import de.samply.annotations.*;
import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheResource;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.email.EmailRecipientType;
import de.samply.email.EmailTemplateType;
import de.samply.frontend.FrontendService;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.modules.OptionalModules;
import de.samply.project.*;
import de.samply.project.event.ProjectEventActionsException;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectState;
import de.samply.project.state.UserProjectState;
import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;
import de.samply.query.QueryService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/** Project (references its Query): listing, design, editing, configuration and the results of the project. */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class ProjectController extends AbstractController {

    private final FrontendService frontendService;
    private final ProjectEventService projectEventService;
    private final QueryService queryService;
    private final ProjectService projectService;
    private final DtoProjectService dtoProjectService;
    private final BridgeheadsConfiguration bridgeheadsConfiguration;
    private final ProjectConfigurations frontendProjectConfigurations;
    private final OptionalModules optionalModules;

    public ProjectController(FrontendService frontendService,
                             ProjectEventService projectEventService,
                             QueryService queryService,
                             ProjectService projectService,
                             DtoProjectService dtoProjectService,
                             BridgeheadsConfiguration bridgeheadsConfiguration,
                             ProjectConfigurations frontendProjectConfigurations,
                             OptionalModules optionalModules) {
        this.frontendService = frontendService;
        this.projectEventService = projectEventService;
        this.queryService = queryService;
        this.projectService = projectService;
        this.dtoProjectService = dtoProjectService;
        this.bridgeheadsConfiguration = bridgeheadsConfiguration;
        this.frontendProjectConfigurations = frontendProjectConfigurations;
        this.optionalModules = optionalModules;
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.PROJECTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECTS_ACTION)
    @CacheCategory(CacheResource.PROJECT_DASHBOARD)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchProjects(
            @RequestParameter(name = ProjectManagerConst.PROJECT_STATE, required = false) ProjectState projectState,
            @RequestParameter(name = ProjectManagerConst.ARCHIVED, required = false) Boolean archived,
            @RequestParameter(name = ProjectManagerConst.SORT_BY, required = false, defaultValue = "created") String sortBy,
            @RequestParameter(name = ProjectManagerConst.SORT_DESC, required = false, defaultValue = "true") boolean sortDesc,
            @RequestParameter(name = ProjectManagerConst.PAGE) int page,
            @RequestParameter(name = ProjectManagerConst.PAGE_SIZE) int pageSize,
            @RequestParameter(name = ProjectManagerConst.PROJECT_CREATOR_EMAIL, required = false) String projectCreatorEmail,
            @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead
    ) {
        return convertToResponseEntity(() -> dtoProjectService.fetchUserVisibleProjects(
                Optional.ofNullable(projectState), Optional.ofNullable(archived), page, pageSize,
                ProjectSortField.fromValue(sortBy), sortDesc,
                Optional.ofNullable(projectCreatorEmail), Optional.ofNullable(bridgehead)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_ACTION)
    @CacheCategory(CacheResource.PROJECT_DETAIL)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchProject(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // Bridgehead only for the site-dependent roles; CREATOR and admins also work without (projects without sites)
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> dtoProjectService.fetchDtoProject(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_TYPES_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_TYPES)
    public ResponseEntity fetchProjectTypes(
            // ProjectCode code needed for role constraints
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        // Only the types whose optional modules are enabled
        return convertToResponseEntity(optionalModules::fetchAvailableProjectTypes);
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.DESIGN_PROJECT)
    public ResponseEntity designProject(
            @RequestVariable(name = ProjectManagerConst.BRIDGEHEADS) String[] bridgeheads,
            @RequestVariable(name = ProjectManagerConst.QUERY_CODE) String queryCode
    ) {
        return convertToResponseEntity(() -> this.projectEventService.draft(bridgeheads, queryCode));
    }

    // Called by the explorer (query and sites) and by the dashboard's "Create request" button
    // (empty query with its format, no sites: the creator defines the query in the project view)
    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.PROJECTS_MODULE)
    // Also in the project view: its success message is shown there after the redirect
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.CREATE_QUERY_AND_DESIGN_PROJECT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.CREATE_QUERY_AND_DESIGN_PROJECT)
    public ResponseEntity createQueryAndDesignProject(
            @RequestVariable(name = ProjectManagerConst.QUERY) String query,
            @RequestVariable(name = ProjectManagerConst.QUERY_FORMAT, notEmpty = true) QueryFormat queryFormat,
            @RequestVariable(name = ProjectManagerConst.BRIDGEHEADS, required = false) String[] bridgeheads,
            @RequestVariable(name = ProjectManagerConst.EXPLORER_IDS, required = false) String[] explorerIds,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestVariable(name = ProjectManagerConst.DESCRIPTION, required = false) String description,
            @RequestVariable(name = ProjectManagerConst.OUTPUT_FORMAT, required = false) OutputFormat outputFormat,
            @RequestVariable(name = ProjectManagerConst.TEMPLATE_ID, required = false) String templateId,
            @RequestVariable(name = ProjectManagerConst.HUMAN_READABLE, required = false) String humanReadable,
            @RequestVariable(name = ProjectManagerConst.REDIRECT_EXPLORER_URL, required = false) String explorerUrl,
            @RequestVariable(name = ProjectManagerConst.PROJECT_TYPE, required = false) ProjectType projectType,
            @RequestVariable(name = ProjectManagerConst.QUERY_DETAILS, required = false) String queryDetails,
            @RequestVariable(name = ProjectManagerConst.QUERY_CONTEXT, required = false) String queryContext
    ) throws ProjectEventActionsException {
        String[] tempBridgeheads = areBridgeheadsOrExplorerIdsEmpty(bridgeheads, explorerIds)
                ? new String[0] : resolveBridgeheads(bridgeheads, explorerIds);
        String queryCode = this.queryService.createQuery(
                query, queryFormat, label, description, outputFormat, templateId,
                projectType, humanReadable, explorerUrl, queryContext, queryDetails);
        String projectCode = this.projectEventService.draft(tempBridgeheads, queryCode);
        this.queryService.addProjectCodeToExporterUrl(queryCode, projectCode);
        // The returned URL is only followed, never stored (unlike the explorer URL of the query)
        return convertToResponseEntity(() -> this.frontendService.fetchExplorerRedirectUri(
                ProjectManagerConst.PROJECT_VIEW_SITE,
                Map.of(ProjectManagerConst.PROJECT_CODE, projectCode,
                        ProjectManagerConst.ACTION_FEEDBACK, ProjectManagerConst.CREATE_QUERY_AND_DESIGN_PROJECT_ACTION)
        ));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.EDIT_PROJECT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.EDIT_PROJECT)
    public ResponseEntity editProject(
            @RequestVariable(name = ProjectManagerConst.QUERY, required = false) String query,
            @RequestVariable(name = ProjectManagerConst.QUERY_FORMAT, required = false) QueryFormat queryFormat,
            @RequestVariable(name = ProjectManagerConst.BRIDGEHEADS, required = false) String[] bridgeheads,
            @RequestVariable(name = ProjectManagerConst.EXPLORER_IDS, required = false) String[] explorerIds,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestVariable(name = ProjectManagerConst.DESCRIPTION, required = false) String description,
            @RequestVariable(name = ProjectManagerConst.COHORT_DEFINITION, required = false) String cohortDefinition,
            @RequestVariable(name = ProjectManagerConst.OUTPUT_FORMAT, required = false) OutputFormat outputFormat,
            @RequestVariable(name = ProjectManagerConst.TEMPLATE_ID, required = false) String templateId,
            @RequestVariable(name = ProjectManagerConst.PROJECT_TYPE, required = false) ProjectType projectType,
            @RequestVariable(name = ProjectManagerConst.HUMAN_READABLE, required = false) String humanReadable,
            @RequestVariable(name = ProjectManagerConst.REDIRECT_EXPLORER_URL, required = false) String explorerUrl,
            @RequestVariable(name = ProjectManagerConst.QUERY_CONTEXT, required = false) String queryContext,
            @RequestVariable(name = ProjectManagerConst.QUERY_DETAILS, required = false) String queryDetails,
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        // Sites are only changed when sent; an empty list removes all of them
        if (bridgeheads != null || explorerIds != null) {
            projectService.updateBridgeheads(project, areBridgeheadsOrExplorerIdsEmpty(bridgeheads, explorerIds)
                    ? new String[0] : resolveBridgeheads(bridgeheads, explorerIds));
        }
        queryService.editQuery(project,
                (query != null && !query.trim().isEmpty() && !query.equals("{}")) ? query :
                        null, queryFormat, label, description, outputFormat, templateId, projectType,
                humanReadable,
                explorerUrl, queryContext, cohortDefinition, queryDetails);
        return convertToResponseEntity(() -> this.frontendService.fetchExplorerRedirectUri(
                ProjectManagerConst.PROJECT_VIEW_SITE,
                Map.of(ProjectManagerConst.PROJECT_CODE, project.getCode(),
                        ProjectManagerConst.ACTION_FEEDBACK, ProjectManagerConst.EDIT_PROJECT_ACTION)
        ));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_CONFIGURATIONS_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_CONFIGURATIONS)
    public ResponseEntity fetchProjectConfigurations(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(this.frontendProjectConfigurations::getConfig);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_CONFIGURATION_SELECTION_TYPE_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_CONFIGURATION_SELECTION_TYPE)
    public ResponseEntity fetchProjectConfigurationSelectionType(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(this.frontendProjectConfigurations::getSelectionType);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_CURRENT_PROJECT_CONFIGURATION_ACTION)
    @CacheCategory(CacheResource.PROJECT_DETAIL)
    @GetMapping(value = ProjectManagerConst.FETCH_CURRENT_PROJECT_CONFIGURATION)
    public ResponseEntity fetchCurrentProjectConfiguration(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(
                () -> this.dtoProjectService.fetchCurrentProjectConfiguration(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_PROJECT_CONFIGURATION_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.SET_PROJECT_CONFIGURATION)
    public ResponseEntity setProjectConfiguration(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestVariable(name = ProjectManagerConst.PROJECT_CONFIGURATION) String projectConfigurationName
    ) {
        return convertToResponseEntity(
                () -> this.projectService.setProjectConfiguration(project, projectConfigurationName));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_RESULTS_ACTION)
    @CacheCategory(CacheResource.PROJECT_RESULTS)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_RESULTS)
    public ResponseEntity fetchProjectResults(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(() -> dtoProjectService.fetchResults(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL}, userProjectStates = {
            UserProjectState.ACCEPTED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @EmailSender(templateType = EmailTemplateType.PROJECT_RESULTS_URL_ADDED, recipients = {
            EmailRecipientType.CREATOR})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_PROJECT_RESULTS_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.ADD_PROJECT_RESULTS_URL)
    public ResponseEntity addProjectResultsUrl(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.RESULTS_URL) String resultsUrl
    ) {
        return convertToResponseEntity(() -> projectService.addProjectResultUrl(project, resultsUrl));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @EmailSender(templateType = EmailTemplateType.PROJECT_RESULTS_ACCEPTED_BY_CREATOR, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_PROJECT_RESULTS_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.ACCEPT_PROJECT_RESULTS_URL)
    public ResponseEntity acceptProjectResultsUrlByCreator(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> projectService.acceptResultsByCreator(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @EmailSender(templateType = EmailTemplateType.PROJECT_RESULTS_REJECTED_BY_CREATOR, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_RESULTS_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.REJECT_PROJECT_RESULTS_URL)
    public ResponseEntity rejectProjectResultsUrlByCreator(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> projectService.rejectResultsForCreator(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @EmailSender(templateType = EmailTemplateType.PROJECT_RESULTS_REQUESTED_CHANGES_BY_CREATOR, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_RESULTS_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_RESULTS_URL)
    public ResponseEntity requestChangesInProjectResultsUrlByCreator(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> projectService.requestChangesInResultsForCreator(project));
    }

    private boolean areBridgeheadsOrExplorerIdsEmpty(String[] bridgeheads, String[] explorerIds) {
        return (bridgeheads == null || bridgeheads.length == 0) && (explorerIds == null
                || explorerIds.length == 0);
    }

    private String[] resolveBridgeheads(String[] bridgeheads, String[] explorerIds) {
        return (explorerIds != null && explorerIds.length > 0)
                ? Arrays.stream(explorerIds)
                .map(bridgeheadsConfiguration::getBridgeheadForExplorerId)
                .flatMap(Optional::stream)
                .toArray(String[]::new)
                : bridgeheads;
    }

}
