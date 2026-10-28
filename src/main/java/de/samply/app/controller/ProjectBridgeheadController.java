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
import de.samply.frontend.dto.DtoFactory;
import de.samply.project.*;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryState;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ProjectBridgehead (a bridgehead of a Project): the bridgeheads, their acceptance of the project and
 * their results.
 */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class ProjectBridgeheadController extends AbstractController {

    private final DtoProjectBridgeheadService dtoProjectBridgeheadService;
    private final BridgeheadsConfiguration bridgeheadsConfiguration;
    private final DtoFactory dtoFactory;
    private final ProjectBridgeheadService projectBridgeheadService;

    public ProjectBridgeheadController(DtoProjectBridgeheadService dtoProjectBridgeheadService,
                                       BridgeheadsConfiguration bridgeheadsConfiguration,
                                       DtoFactory dtoFactory,
                                       ProjectBridgeheadService projectBridgeheadService) {
        this.dtoProjectBridgeheadService = dtoProjectBridgeheadService;
        this.bridgeheadsConfiguration = bridgeheadsConfiguration;
        this.dtoFactory = dtoFactory;
        this.projectBridgeheadService = projectBridgeheadService;
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_ALL_REGISTERED_BRIDGEHEADS_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_ALL_REGISTERED_BRIDGEHEADS)
    public ResponseEntity fetchAllRegisteredBridgeheads() {
        return convertToResponseEntity(() -> bridgeheadsConfiguration
                .getRegisteredBridgeheads().stream().map(dtoFactory::convertToBridgehead)
                .collect(Collectors.toSet()));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.PROJECTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_VISIBLE_BRIDGEHEADS_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_VISIBLE_BRIDGEHEADS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchVisibleBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project
    ) {
        return convertToResponseEntity(
                () -> dtoProjectBridgeheadService.fetchUserVisibleProjectBridgeheads(Optional.ofNullable(project)));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEADS_ACTION)
    @CacheCategory(CacheResource.PROJECT_BRIDGEHEAD_EXECUTIONS)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEADS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchProjectsBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(
                () -> dtoProjectBridgeheadService.fetchProjectBridgeheads(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT,
            ProjectState.FINAL}, queryStates = {QueryState.FINISHED, QueryState.ERROR})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BRIDGEHEAD_ACCEPTED, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_BRIDGEHEAD_PROJECT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.ACCEPT_BRIDGEHEAD_PROJECT)
    public ResponseEntity acceptBridgeheadProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> projectBridgeheadService.acceptProject(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT,
            ProjectState.FINAL}, queryStates = {QueryState.FINISHED, QueryState.ERROR})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BRIDGEHEAD_REJECTED, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_BRIDGEHEAD_PROJECT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.REJECT_BRIDGEHEAD_PROJECT)
    public ResponseEntity rejectBridgeheadProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            // Message is sent per email
            @SuppressWarnings("unused") @Message @RequestVariable(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(
                () -> projectBridgeheadService.rejectProject(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEAD_RESULTS_ACTION)
    @CacheCategory(CacheResource.PROJECT_RESULTS)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEAD_RESULTS)
    public ResponseEntity fetchProjectBridgeheadResults(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> dtoProjectBridgeheadService.fetchResults(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEAD_RESULTS_FOR_OWN_BRIDGEHEAD_ACTION)
    @CacheCategory(CacheResource.PROJECT_RESULTS)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEAD_RESULTS_FOR_OWN_BRIDGEHEAD)
    public ResponseEntity fetchProjectBridgeheadResultsForOwnBridgehead(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(() -> Optional.of(
                dtoProjectBridgeheadService.fetchResultsOfOwnBridgehead(bridgehead)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINAL}, projectBridgeheadStates = {
            ProjectBridgeheadState.ACCEPTED})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BRIDGEHEAD_RESULTS_URL_ADDED, recipients = {
            EmailRecipientType.ALL_FINALS, EmailRecipientType.CREATOR})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_PROJECT_BRIDGEHEAD_RESULTS_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.ADD_PROJECT_BRIDGEHEAD_RESULTS_URL)
    public ResponseEntity addProjectBridgeheadResultsUrl(
            @SuppressWarnings("unused") @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.RESULTS_URL) String resultsUrl
    ) {
        return convertToResponseEntity(
                () -> projectBridgeheadService.addResultsUrl(bridgehead, resultsUrl));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BRIDGEHEAD_RESULTS_ACCEPTED_BY_CREATOR, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.BRIDGEHEAD_ADMIN,
            EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_PROJECT_BRIDGEHEAD_RESULTS_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.ACCEPT_PROJECT_BRIDGEHEAD_RESULTS_URL)
    public ResponseEntity acceptProjectBridgeheadResultsUrlByCreator(
            @SuppressWarnings("unused") @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> projectBridgeheadService.acceptResultsForCreator(bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BRIDGEHEAD_RESULTS_REJECTED_BY_CREATOR, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.BRIDGEHEAD_ADMIN,
            EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_BRIDGEHEAD_RESULTS_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.REJECT_PROJECT_BRIDGEHEAD_RESULTS_URL)
    public ResponseEntity rejectProjectBridgeheadResultsUrlByCreator(
            @SuppressWarnings("unused") @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> projectBridgeheadService.rejectResultsForCreator(bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BRIDGEHEAD_RESULTS_REQUESTED_CHANGES_BY_CREATOR, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.BRIDGEHEAD_ADMIN,
            EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_BRIDGEHEAD_RESULTS_URL_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_BRIDGEHEAD_RESULTS_URL)
    public ResponseEntity requestChangesProjectBridgeheadResultsUrlByCreator(
            @SuppressWarnings("unused") @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> projectBridgeheadService.requestChangesInResultsForCreator(bridgehead));
    }

}
