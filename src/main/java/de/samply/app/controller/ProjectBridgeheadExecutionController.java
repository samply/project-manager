package de.samply.app.controller;

import de.samply.annotations.*;
import de.samply.app.ProjectManagerConst;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheResource;
import de.samply.datashield.DataShieldService;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.exporter.ExporterService;
import de.samply.feasibility.FeasibilityService;
import de.samply.modules.OptionalModule;
import de.samply.modules.OptionalModules;
import de.samply.project.*;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryFormat;
import de.samply.query.QueryState;
import de.samply.researchenvironment.ResearchEnvironmentService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * ProjectBridgeheadExecution and ProjectBridgeheadDataShield of a ProjectBridgehead: feasibility, query
 * in the bridgehead, research environment and DataSHIELD.
 */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class ProjectBridgeheadExecutionController extends AbstractController {

    private final ExporterService exporterService;
    private final OptionalModules optionalModules;
    private final ProjectBridgeheadService projectBridgeheadService;
    private final DataShieldService dataShieldService;
    private final ResearchEnvironmentService researchEnvironmentService;
    private final FeasibilityService feasibilityService;

    public ProjectBridgeheadExecutionController(ExporterService exporterService,
                                                OptionalModules optionalModules,
                                                ProjectBridgeheadService projectBridgeheadService,
                                                DataShieldService dataShieldService,
                                                ResearchEnvironmentService researchEnvironmentService,
                                                FeasibilityService feasibilityService) {
        this.exporterService = exporterService;
        this.optionalModules = optionalModules;
        this.projectBridgeheadService = projectBridgeheadService;
        this.dataShieldService = dataShieldService;
        this.researchEnvironmentService = researchEnvironmentService;
        this.feasibilityService = feasibilityService;
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE,
            module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.IS_FEASIBILITY_ENABLED_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.IS_FEASIBILITY_ENABLED,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity isFeasibilityEnabled() {
        return convertToResponseEntity(() -> optionalModules.isEnabled(OptionalModule.FEASIBILITY));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @ProjectConstraints(queryFormats = {QueryFormat.AST_DATA})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE,
            module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_FEASIBILITY_ACTION)
    @CacheCategory(CacheResource.FEASIBILITY_STATISTICS)
    @GetMapping(value = ProjectManagerConst.FETCH_FEASIBILITY,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchFeasibility(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() ->
                feasibilityService.fetchFeasibility(project, bridgehead).block());
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL},
            queryStates = {QueryState.CREATED, QueryState.ERROR, QueryState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.SAVE_QUERY_IN_BRIDGEHEAD_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.SAVE_QUERY_IN_BRIDGEHEAD)
    public ResponseEntity saveQueryInBridgehead(
            @SuppressWarnings("unused") @NotEmpty @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @NotEmpty @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.PROJECT_TYPE) List<ProjectType> projectTypes
    ) {
        return convertToResponseEntity(() -> Optional
                .ofNullable(projectTypes)
                .ifPresent(types -> types
                        .forEach(type ->
                                this.projectBridgeheadService.scheduleSendQueryToBridgehead(bridgehead, type))));

    }

    // Only with ENABLE_EXTERNAL_EXECUTION=true (disabled by default): otherwise neither offered nor accepted
    @RequiresModule(OptionalModule.EXTERNAL_EXECUTION)
    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL},
            queryStates = {QueryState.CREATED, QueryState.ERROR, QueryState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD)
    public ResponseEntity saveAndExecuteQueryInBridgehead(
            @SuppressWarnings("unused") @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.PROJECT_TYPE) List<ProjectType> projectTypes
    ) {
        return convertToResponseEntity(() -> Optional
                .ofNullable(projectTypes)
                .ifPresent(types -> types
                        .forEach(type ->
                                this.projectBridgeheadService.scheduleSendQueryToBridgeheadAndExecute(bridgehead,
                                        type))));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_RESEARCH_ENVIRONMENT_URL_ACTION)
    @CacheCategory(CacheResource.PROJECT_BRIDGEHEAD_EXECUTIONS)
    @GetMapping(value = ProjectManagerConst.FETCH_RESEARCH_ENVIRONMENT_URL)
    public ResponseEntity fetchResearchEnvironmentUrl(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(researchEnvironmentService::fetchResearchEnvironmentUrl);
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_RESEARCH_ENVIRONMENT_WORKSPACE_ACTION)
    @CacheCategory(CacheResource.PROJECT_BRIDGEHEAD_EXECUTIONS)
    @GetMapping(value = ProjectManagerConst.EXISTS_RESEARCH_ENVIRONMENT_WORKSPACE)
    public ResponseEntity existsUserResearchEnvironmentWorkspace(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> researchEnvironmentService.existsUserResearchEnvironmentWorkspace(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL},
            queryStates = {QueryState.FINISHED})
    @ProjectConstraints(projectTypes = {ProjectType.RESEARCH_ENVIRONMENT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.SEND_EXPORT_FILES_TO_RESEARCH_ENVIRONMENT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.SEND_EXPORT_FILES_TO_RESEARCH_ENVIRONMENT)
    public ResponseEntity sendExportFilesToResearchEnvironment(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> exporterService.transferFileToResearchEnvironment(project.getCode(),
                        bridgehead.getBridgehead()));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL},
            queryStates = {QueryState.FINISHED})
    @ProjectConstraints(projectTypes = {ProjectType.RESEARCH_ENVIRONMENT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.ARE_EXPORT_FILES_TRANSFERRED_TO_RESEARCH_ENVIRONMENT_ACTION)
    @CacheCategory(CacheResource.PROJECT_BRIDGEHEAD_EXECUTIONS)
    @GetMapping(value = ProjectManagerConst.ARE_EXPORT_FILES_TRANSFERRED_TO_RESEARCH_ENVIRONMENT)
    public ResponseEntity isExportFileTransferredToResearchEnvironment(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> exporterService.isExportFileTransferredToResearchEnvironment(project.getCode(),
                        bridgehead.getBridgehead()));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.TOKEN_MANAGER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_DATASHIELD_STATUS_ACTION)
    @CacheCategory(CacheResource.PROJECT_BRIDGEHEAD_EXECUTIONS)
    @GetMapping(value = ProjectManagerConst.FETCH_DATASHIELD_STATUS)
    public ResponseEntity fetchDatashieldStatus(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> this.dataShieldService.fetchProjectStatus(project, bridgehead).block());
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.TOKEN_MANAGER_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_AUTHENTICATION_SCRIPT_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_AUTHENTICATION_SCRIPT)
    public ResponseEntity fetchTokenScript(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        ProjectManagerConst.AUTHENTICATION_SCRIPT_FILENAME_PREFIX + project.getCode()
                        + ProjectManagerConst.AUTHENTICATION_SCRIPT_FILENAME_SUFFIX + "\"")
                .body(this.dataShieldService.fetchAuthenticationScript(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.TOKEN_MANAGER_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_AUTHENTICATION_SCRIPT_ACTION)
    @CacheCategory(CacheResource.PROJECT_DOCUMENTS)
    @GetMapping(value = ProjectManagerConst.EXISTS_AUTHENTICATION_SCRIPT)
    public ResponseEntity existsTokenScript(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> this.dataShieldService.existsAuthenticationScript(project, bridgehead));
    }

}
