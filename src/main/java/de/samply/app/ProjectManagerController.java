package de.samply.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.annotations.*;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.ProjectDocument;
import de.samply.document.DocumentService;
import de.samply.document.DocumentServiceException;
import de.samply.document.DocumentType;
import de.samply.email.EmailRecipientType;
import de.samply.email.EmailTemplateType;
import de.samply.exporter.ExporterService;
import de.samply.frontend.FrontendService;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.notification.NotificationService;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectService;
import de.samply.project.ProjectType;
import de.samply.project.event.ProjectEventActionsException;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectState;
import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;
import de.samply.query.QueryService;
import de.samply.query.QueryState;
import de.samply.token.DataShieldTokenManagerService;
import de.samply.user.UserService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.ProjectVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class ProjectManagerController {

    private final String projectVersion = ProjectVersion.getProjectVersion();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    private final ProjectEventService projectEventService;
    private final FrontendService frontendService;
    private final UserService userService;
    private final QueryService queryService;
    private final DocumentService documentService;
    private final ExporterService exporterService;
    private final DataShieldTokenManagerService dataShieldTokenManagerService;
    private final ProjectService projectService;
    private final ProjectBridgeheadService projectBridgeheadService;
    private final NotificationService notificationService;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final ProjectConfigurations frontendProjectConfigurations;
    private final DtoFactory dtoFactory;

    public ProjectManagerController(ProjectEventService projectEventService,
                                    FrontendService frontendService,
                                    UserService userService,
                                    QueryService queryService,
                                    DocumentService documentService,
                                    ExporterService exporterService,
                                    DataShieldTokenManagerService dataShieldTokenManagerService,
                                    ProjectService projectService,
                                    ProjectBridgeheadService projectBridgeheadService,
                                    NotificationService notificationService,
                                    BridgeheadConfiguration bridgeheadConfiguration,
                                    ProjectConfigurations frontendProjectConfigurations,
                                    DtoFactory dtoFactory) {
        this.projectEventService = projectEventService;
        this.frontendService = frontendService;
        this.userService = userService;
        this.queryService = queryService;
        this.documentService = documentService;
        this.exporterService = exporterService;
        this.dataShieldTokenManagerService = dataShieldTokenManagerService;
        this.projectService = projectService;
        this.projectBridgeheadService = projectBridgeheadService;
        this.notificationService = notificationService;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.frontendProjectConfigurations = frontendProjectConfigurations;
        this.dtoFactory = dtoFactory;
    }

    @GetMapping(value = ProjectManagerConst.INFO)
    public ResponseEntity<String> info() {
        return new ResponseEntity<>(projectVersion, HttpStatus.OK);
    }

    @GetMapping(value = ProjectManagerConst.TEST)
    public ResponseEntity<String> test() {
        return new ResponseEntity<>(ProjectManagerConst.THIS_IS_A_TEST, HttpStatus.OK);
    }

    @GetMapping(value = ProjectManagerConst.ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchActions(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE, required = false) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.SITE) String site
    ) {
        return convertToResponseEntity(() ->
                this.frontendService.fetchModuleActionPackage(site, Optional.ofNullable(projectCode), Optional.ofNullable(bridgehead), true));
    }

    @GetMapping(value = ProjectManagerConst.ALL_ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchAllActions(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE, required = false) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.SITE) String site
    ) {
        return convertToResponseEntity(() ->
                this.frontendService.fetchModuleActionPackage(site, Optional.ofNullable(projectCode), Optional.ofNullable(bridgehead), false));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.PROJECTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECTS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchProjects(
            @RequestParam(name = ProjectManagerConst.PROJECT_STATE, required = false) ProjectState projectState,
            @RequestParam(name = ProjectManagerConst.ARCHIVED, required = false) Boolean archived,
            @RequestParam(name = ProjectManagerConst.LAST_MODIFIED_DESC, required = false, defaultValue = "true") boolean modifiedDescendant,
            @RequestParam(name = ProjectManagerConst.PAGE) int page,
            @RequestParam(name = ProjectManagerConst.PAGE_SIZE) int pageSize
    ) {
        return convertToResponseEntity(() -> projectService.fetchUserVisibleProjects(
                Optional.ofNullable(projectState), Optional.ofNullable(archived), page, pageSize, modifiedDescendant));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // Bridgehead required for role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> projectService.fetchProject(projectCode));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_STATES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_STATES, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchProjectStates(
    ) {
        return convertToResponseEntity(() -> ProjectState.values());
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_VISIBLE_PROJECT_BRIDGEHEADS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_VISIBLE_PROJECT_BRIDGEHEADS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchVisibleProjectsBridgeheads(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectBridgeheadService.fetchUserVisibleProjectBridgeheads(projectCode));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEADS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEADS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchProjectsBridgeheads(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectBridgeheadService.fetchProjectBridgeheads(projectCode));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT})
    @EmailSender(templateType = EmailTemplateType.INVITATION, recipients = {EmailRecipientType.EMAIL_ANNOTATION})
    @EmailSender(templateType = EmailTemplateType.REQUEST_TECHNICAL_APPROVAL, recipients = {EmailRecipientType.BRIDGEHEAD_ADMIN})
    //TODO: Send email to PM-ADMIN, that there was a problem with the operation
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_DEVELOPER_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_DEVELOPER_USER)
    public ResponseEntity<String> setUserAsDeveloper(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @Email @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgheadUserWithRoleAndGenerateTokensIfDataShield(email, projectCode, bridgehead, ProjectRole.DEVELOPER));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT})
    @EmailSender(templateType = EmailTemplateType.INVITATION, recipients = {EmailRecipientType.EMAIL_ANNOTATION})
    @EmailSender(templateType = EmailTemplateType.REQUEST_TECHNICAL_APPROVAL, recipients = {EmailRecipientType.BRIDGEHEAD_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_PILOT_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_PILOT_USER)
    public ResponseEntity<String> setUserAsPilot(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @Email @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgheadUserWithRoleAndGenerateTokensIfDataShield(email, projectCode, bridgehead, ProjectRole.PILOT));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.INVITATION, recipients = {EmailRecipientType.EMAIL_ANNOTATION})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_FINAL_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_FINAL_USER)
    public ResponseEntity<String> setUserAsFinal(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @Email @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgheadUserWithRoleAndGenerateTokensIfDataShield(email, projectCode, bridgehead, ProjectRole.FINAL));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @PostMapping(value = ProjectManagerConst.CREATE_QUERY)
    public ResponseEntity<String> createProjectQuery(
            @NotEmpty @RequestBody() String query,
            @NotEmpty @RequestParam(name = ProjectManagerConst.QUERY_FORMAT) QueryFormat queryFormat,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParam(name = ProjectManagerConst.DESCRIPTION, required = false) String description,
            @RequestParam(name = ProjectManagerConst.OUTPUT_FORMAT, required = false) OutputFormat outputFormat,
            @RequestParam(name = ProjectManagerConst.TEMPLATE_ID, required = false) String templateId,
            @RequestParam(name = ProjectManagerConst.HUMAN_READABLE, required = false) String humanReadable,
            @RequestParam(name = ProjectManagerConst.REDIRECT_EXPLORER_URL, required = false) String explorerUrl,
            @RequestParam(name = ProjectManagerConst.QUERY_CONTEXT, required = false) String queryContext
    ) {
        return convertToResponseEntity(() ->
                this.queryService.createQuery(query, queryFormat, label, description, outputFormat, templateId, humanReadable, explorerUrl, queryContext));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @PostMapping(value = ProjectManagerConst.CREATE_QUERY_AND_DESIGN_PROJECT)
    public ResponseEntity<String> createQueryAndDesignProject(
            @NotEmpty @RequestBody() String query,
            @NotEmpty @RequestParam(name = ProjectManagerConst.QUERY_FORMAT) QueryFormat queryFormat,
            @RequestParam(name = ProjectManagerConst.BRIDGEHEADS, required = false) String[] bridgeheads,
            @RequestParam(name = ProjectManagerConst.EXPLORER_IDS, required = false) String[] explorerIds,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParam(name = ProjectManagerConst.DESCRIPTION, required = false) String description,
            @RequestParam(name = ProjectManagerConst.OUTPUT_FORMAT, required = false) OutputFormat outputFormat,
            @RequestParam(name = ProjectManagerConst.TEMPLATE_ID, required = false) String templateId,
            @RequestParam(name = ProjectManagerConst.HUMAN_READABLE, required = false) String humanReadable,
            @RequestParam(name = ProjectManagerConst.REDIRECT_EXPLORER_URL, required = false) String explorerUrl,
            @RequestParam(name = ProjectManagerConst.PROJECT_TYPE, required = false) ProjectType projectType,
            @RequestParam(name = ProjectManagerConst.QUERY_CONTEXT, required = false) String queryContext
    ) throws ProjectEventActionsException {
        if (areBridgeheadsOrExplorerIdsEmpty(bridgeheads, explorerIds)) {
            return ResponseEntity.badRequest().body("Bridgeheads or explorer ids cannot be empty");
        }
        String[] tempBridgeheads = (explorerIds != null && explorerIds.length > 0) ?
                Arrays.stream(explorerIds).map(explorerId ->
                        bridgeheadConfiguration.getBridgeheadForExplorerId(explorerId).get()).toArray(String[]::new) : bridgeheads;
        String queryCode = this.queryService.createQuery(
                query, queryFormat, label, description, outputFormat, templateId, humanReadable, explorerUrl, queryContext);
        String projectCode = this.projectEventService.draft(tempBridgeheads, queryCode, projectType);
        this.queryService.addProjectCodeToExporterUrl(queryCode, projectCode);
        return convertToResponseEntity(() -> this.frontendService.fetchExplorerRedirectUri(
                ProjectManagerConst.PROJECT_VIEW_SITE,
                Map.of(ProjectManagerConst.PROJECT_CODE, projectCode)
        ));
    }

    private boolean areBridgeheadsOrExplorerIdsEmpty(String[] bridgeheads, String[] explorerIds) {
        return (bridgeheads == null || bridgeheads.length == 0) && (explorerIds == null || explorerIds.length == 0);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.EDIT_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.EDIT_PROJECT)
    public ResponseEntity<String> editProject(
            @RequestBody() String query,
            @RequestParam(name = ProjectManagerConst.QUERY_FORMAT, required = false) QueryFormat queryFormat,
            @RequestParam(name = ProjectManagerConst.BRIDGEHEADS, required = false) String[] bridgeheads,
            @RequestParam(name = ProjectManagerConst.EXPLORER_IDS, required = false) String[] explorerIds,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParam(name = ProjectManagerConst.DESCRIPTION, required = false) String description,
            @RequestParam(name = ProjectManagerConst.OUTPUT_FORMAT, required = false) OutputFormat outputFormat,
            @RequestParam(name = ProjectManagerConst.TEMPLATE_ID, required = false) String templateId,
            @RequestParam(name = ProjectManagerConst.HUMAN_READABLE, required = false) String humanReadable,
            @RequestParam(name = ProjectManagerConst.REDIRECT_EXPLORER_URL, required = false) String explorerUrl,
            @RequestParam(name = ProjectManagerConst.PROJECT_TYPE, required = false) ProjectType projectType,
            @RequestParam(name = ProjectManagerConst.QUERY_CONTEXT, required = false) String queryContext,
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        String[] tempBridgeheads = (explorerIds != null && explorerIds.length > 0) ?
                Arrays.stream(explorerIds).map(explorerId ->
                        bridgeheadConfiguration.getBridgeheadForExplorerId(explorerId).get()).toArray(String[]::new) : bridgeheads;
        projectService.editProject(projectCode, projectType, tempBridgeheads);
        queryService.editQuery(projectCode, (query != null && query.trim().length() > 0 && !query.equals("{}")) ? query : null, queryFormat, label, description, outputFormat, templateId, humanReadable, explorerUrl, queryContext);
        return convertToResponseEntity(() -> this.frontendService.fetchExplorerRedirectUri(
                ProjectManagerConst.PROJECT_VIEW_SITE,
                Map.of(ProjectManagerConst.PROJECT_CODE, projectCode)
        ));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_EXPORTER_TEMPLATES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_EXPORTER_TEMPLATES)
    public ResponseEntity<String> fetchExporterTemplates(
            // Project code needed for role constraints
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @RequestParam(name = ProjectManagerConst.PROJECT_TYPE) ProjectType projectType
    ) {
        return convertToResponseEntity(() -> exporterService.getExporterTemplates(projectType));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_QUERY_FORMATS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_QUERY_FORMATS)
    public ResponseEntity<String> fetchQueryFormats(
            // Project code needed for role constraints
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> QueryFormat.values());
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_ROLES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_ROLES)
    public ResponseEntity<String> fetchProjectRoles(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead
    ) {
        //TODO
        return convertToResponseEntity(() -> userService.fetchProjectRoles(projectCode, Optional.ofNullable(bridgehead)));
    }


    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_OUTPUT_FORMATS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_OUTPUT_FORMATS)
    public ResponseEntity<String> fetchOutputFormats(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectService.fetchOutputFormats(projectCode));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_CONFIGURATIONS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_CONFIGURATIONS)
    public ResponseEntity<String> fetchProjectConfigurations(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> this.frontendProjectConfigurations.getConfigurationNameProjectMap().keySet());
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_CURRENT_PROJECT_CONFIGURATION_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_CURRENT_PROJECT_CONFIGURATION)
    public ResponseEntity<String> fetchCurrentProjectConfiguration(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> this.projectService.fetchCurrentProjectConfiguration(projectCode));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_PROJECT_CONFIGURATION_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_PROJECT_CONFIGURATION)
    public ResponseEntity<String> setProjectConfiguration(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @RequestParam(name = ProjectManagerConst.PROJECT_CONFIGURATION) String projectConfigurationName
    ) {
        return convertToResponseEntity(() -> this.projectService.setProjectConfiguration(projectCode, projectConfigurationName));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_TYPES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_TYPES)
    public ResponseEntity<String> fetchProjectTypes(
            // Project code needed for role constraints
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> ProjectType.values());
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @PostMapping(value = ProjectManagerConst.DESIGN_PROJECT)
    public ResponseEntity<String> designProject(
            @RequestParam(name = ProjectManagerConst.BRIDGEHEADS) String[] bridgeheads,
            @RequestParam(name = ProjectManagerConst.QUERY_CODE) String queryCode,
            @RequestParam(name = ProjectManagerConst.PROJECT_TYPE) ProjectType projectType
    ) {
        return convertToResponseEntity(() -> this.projectEventService.draft(bridgeheads, queryCode, projectType));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT})
    @EmailSender(templateType = EmailTemplateType.NEW_PROJECT, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.CREATE_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.CREATE_PROJECT)
    public ResponseEntity<String> createProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectEventService.create(projectCode));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.ACCEPTED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_DEVELOP_STAGE_ACTION)
    @PostMapping(value = ProjectManagerConst.START_DEVELOP_STAGE)
    public ResponseEntity<String> startDevelopStage(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectEventService.startDevelopStage(projectCode));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_PILOT_STAGE_ACTION)
    @PostMapping(value = ProjectManagerConst.START_PILOT_STAGE)
    public ResponseEntity<String> startPilotStage(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectEventService.startPilotStage(projectCode));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.PILOT, ProjectState.ACCEPTED})
    @EmailSender(templateType = EmailTemplateType.REQUEST_TECHNICAL_APPROVAL, recipients = {EmailRecipientType.BRIDGHEAD_ADMINS_WHO_HAVE_NOT_ACCEPTED_NOR_REJECTED_THE_PROJECT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_FINAL_STAGE_ACTION)
    @PostMapping(value = ProjectManagerConst.START_FINAL_STAGE)
    public ResponseEntity<String> startFinalStage(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectEventService.startFinalStage(projectCode));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.CREATED, ProjectState.ARCHIVED})
    @EmailSender(templateType = EmailTemplateType.NEW_PROJECT_ACCEPTED, recipients = {EmailRecipientType.CREATOR})
    @EmailSender(templateType = EmailTemplateType.NEW_PROJECT_ACCEPTED, recipients = {EmailRecipientType.ALL_BRIDGEHEAD_ADMINS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.ACCEPT_PROJECT)
    public ResponseEntity<String> acceptProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectEventService.accept(projectCode));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.REJECT_PROJECT)
    public ResponseEntity<String> rejectProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // Message is sent per email
            @Message @RequestParam(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> projectEventService.reject(projectCode));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL}, queryStates = {QueryState.FINISHED, QueryState.ERROR})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BRIDGEHEAD_ACCEPTED, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_BRIDGEHEAD_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.ACCEPT_BRIDGEHEAD_PROJECT)
    public ResponseEntity<String> acceptBridgeheadProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> projectBridgeheadService.acceptProject(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL}, queryStates = {QueryState.FINISHED, QueryState.ERROR})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BRIDGEHEAD_REJECTED, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_BRIDGEHEAD_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.REJECT_BRIDGEHEAD_PROJECT)
    public ResponseEntity<String> rejectBridgeheadProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            // Message is sent per email
            @Message @RequestParam(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> projectBridgeheadService.rejectProject(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @EmailSender(templateType = EmailTemplateType.SCRIPT_ACCEPTED, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS, EmailRecipientType.ALL_PILOTS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_SCRIPT_ACTION)
    @PostMapping(value = ProjectManagerConst.ACCEPT_SCRIPT)
    public ResponseEntity<String> acceptScript(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> userService.acceptProject(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @EmailSender(templateType = EmailTemplateType.SCRIPT_REJECTED, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS, EmailRecipientType.ALL_PILOTS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_SCRIPT_ACTION)
    @PostMapping(value = ProjectManagerConst.REJECT_SCRIPT)
    public ResponseEntity<String> rejectScript(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            // Message is sent per email
            @Message @RequestParam(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.rejectProject(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @EmailSender(templateType = EmailTemplateType.REQUEST_CHANGES_IN_SCRIPT, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS, EmailRecipientType.ALL_PILOTS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REQUEST_SCRIPT_CHANGES_ACTION)
    @PostMapping(value = ProjectManagerConst.REQUEST_SCRIPT_CHANGES)
    public ResponseEntity<String> requestChangesInScript(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            // Message is sent per email
            @Message @RequestParam(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.requestChangesInProject(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.RESULTS_ACCEPTED, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_PROJECT_RESULTS_ACTION)
    @PostMapping(value = ProjectManagerConst.ACCEPT_PROJECT_RESULTS)
    public ResponseEntity<String> acceptProjectResults(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> userService.acceptProject(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.RESULTS_REJECTED, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_RESULTS_ACTION)
    @PostMapping(value = ProjectManagerConst.REJECT_PROJECT_RESULTS)
    public ResponseEntity<String> rejectProjectResults(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            // Message is sent per email
            @Message @RequestParam(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.rejectProject(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.REQUEST_CHANGES_IN_PROJECT, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT)
    public ResponseEntity<String> requestChangesInProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            // Message is sent per email
            @Message @RequestParam(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.requestChangesInProject(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @EmailSender(templateType = EmailTemplateType.ANALYSIS_ACCEPTED, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS, EmailRecipientType.ALL_PILOTS, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_PROJECT_ANALYSIS_ACTION)
    @PostMapping(value = ProjectManagerConst.ACCEPT_PROJECT_ANALYSIS)
    public ResponseEntity<String> acceptProjectAnalysis(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> userService.acceptProject(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @EmailSender(templateType = EmailTemplateType.ANALYSIS_REJECTED, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS, EmailRecipientType.ALL_PILOTS, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_ANALYSIS_ACTION)
    @PostMapping(value = ProjectManagerConst.REJECT_PROJECT_ANALYSIS)
    public ResponseEntity<String> rejectProjectAnalysis(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            // Message is sent per email
            @Message @RequestParam(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.rejectProject(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @EmailSender(templateType = EmailTemplateType.REQUEST_CHANGES_IN_PROJECT_ANALYSIS, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS, EmailRecipientType.ALL_PILOTS, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_ANALYSIS_ACTION)
    @PostMapping(value = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_ANALYSIS)
    public ResponseEntity<String> requestChangesInProjectAnalysis(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            // Message is sent per email
            @Message @RequestParam(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.requestChangesInProject(projectCode, bridgehead));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.CREATED, ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ARCHIVE_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.ARCHIVE_PROJECT)
    public ResponseEntity<String> archiveProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectEventService.archive(projectCode));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.FINISHED_PROJECT, recipients = {EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.FINISH_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.FINISH_PROJECT)
    public ResponseEntity<String> finishProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectEventService.finish(projectCode));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_OTHER_DOCUMENT_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_OTHER_DOCUMENT)
    public ResponseEntity<String> uploadOtherDocument(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // bridgehead required for identifying developer, pilot, final user or bridgehead admin in role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParam(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                projectCode, Optional.of(bridgehead), document, DocumentType.OTHERS, Optional.ofNullable(label)));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER, OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @EmailSender(templateType = EmailTemplateType.NEW_PUBLICATION, recipients = {EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_PUBLICATION_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_PUBLICATION)
    public ResponseEntity<String> uploadPublication(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParam(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                projectCode, Optional.empty(), document, DocumentType.PUBLICATION, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_SCRIPT_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_SCRIPT)
    public ResponseEntity<String> uploadScript(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParam(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                projectCode, Optional.empty(), document, DocumentType.SCRIPT, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_APPLICATION_FORM_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_APPLICATION_FORM)
    public ResponseEntity<String> uploadApplicationForm(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // bridgehead required for identifying bridgehead admin in role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParam(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                projectCode, Optional.empty(), document, DocumentType.APPLICATION_FORM, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.CREATED, ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.NEW_VOTUM, recipients = {EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_VOTUM_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_VOTUM)
    public ResponseEntity<String> uploadVotum(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParam(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                projectCode, Optional.ofNullable(bridgehead), document, DocumentType.VOTUM, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @EmailSender(templateType = EmailTemplateType.NEW_PUBLICATION, recipients = {EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_PUBLICATION_URL_ACTION)
    @PostMapping(value = ProjectManagerConst.ADD_PUBLICATION_URL)
    public ResponseEntity<String> addPublicationUrl(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // bridgehead required for identifying bridgehead admin in role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(
                projectCode, Optional.empty(), documentUrl, DocumentType.PUBLICATION, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_OTHER_DOCUMENT_URL_ACTION)
    @PostMapping(value = ProjectManagerConst.ADD_OTHER_DOCUMENT_URL)
    public ResponseEntity<String> addOtherDocumentUrl(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(
                projectCode, Optional.ofNullable(bridgehead), documentUrl, DocumentType.OTHERS, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_SCRIPT_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_SCRIPT)
    public ResponseEntity<Resource> downloadScript(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(projectCode, null, DocumentType.SCRIPT);
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_SCRIPT_LABEL_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_SCRIPT_LABEL)
    public ResponseEntity<String> fetchScriptLabel(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.documentService.fetchLabelOfLastDocumentOfThisType(projectCode, Optional.empty(), DocumentType.SCRIPT));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_SCRIPT_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_SCRIPT)
    public ResponseEntity<Boolean> existsScript(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead
    ) throws DocumentServiceException {
        return existsProjectDocument(projectCode, null, DocumentType.SCRIPT);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.CREATED, ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_VOTUM_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_VOTUM)
    public ResponseEntity<Resource> downloadVotum(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(projectCode, bridgehead, DocumentType.VOTUM);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.CREATED, ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_VOTUM_LABEL_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_VOTUM_LABEL)
    public ResponseEntity<String> fetchVotumLabel(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.documentService.fetchLabelOfLastDocumentOfThisType(projectCode, Optional.ofNullable(bridgehead), DocumentType.VOTUM));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.CREATED, ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_VOTUM_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_VOTUM)
    public ResponseEntity<Boolean> existsVotum(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) throws DocumentServiceException {
        return existsProjectDocument(projectCode, bridgehead, DocumentType.VOTUM);
    }


    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED, ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_APPLICATION_FORM_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_APPLICATION_FORM)
    public ResponseEntity<Resource> downloadApplicationForm(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // bridgehead required for identifying bridgehead admin in role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(projectCode, null, DocumentType.APPLICATION_FORM);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED, ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_APPLICATION_FORM_LABEL_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_APPLICATION_FORM_LABEL)
    public ResponseEntity<String> fetchApplicationFormLabel(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // bridgehead required for identifying bridgehead admin in role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.documentService.fetchLabelOfLastDocumentOfThisType(projectCode, Optional.empty(), DocumentType.APPLICATION_FORM));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED, ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_APPLICATION_FORM_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_APPLICATION_FORM)
    public ResponseEntity<Boolean> existsApplicationForm(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            // bridgehead required for identifying bridgehead admin in role constraints
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead
    ) throws DocumentServiceException {
        return existsProjectDocument(projectCode, null, DocumentType.APPLICATION_FORM);
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @StateConstraints(projectStates = {ProjectState.DRAFT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_APPLICATION_FORM_TEMPLATE_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_APPLICATION_FORM_TEMPLATE)
    public ResponseEntity<Resource> downloadApplicationFormTemplate(
            // Project code is needed for the project constraint.
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) throws DocumentServiceException {
        Optional<Path> filePath = documentService.fetchApplicationForm();
        return (filePath.isEmpty()) ? ResponseEntity.notFound().build() :
                downloadDocument(filePath.get().getFileName().toString(), filePath.get());
    }

    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_PUBLICATION_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_PUBLICATION)
    public ResponseEntity<Resource> downloadPublication(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @RequestParam(name = ProjectManagerConst.FILENAME) String filename
    ) throws DocumentServiceException {
        return downloadProjectDocument(projectCode, null, filename, DocumentType.PUBLICATION);
    }

    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PUBLICATIONS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PUBLICATIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchPublications(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> documentService.fetchPublications(projectCode));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_OTHER_DOCUMENT_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_OTHER_DOCUMENT)
    public ResponseEntity<Resource> downloadOtherProjectDocument(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.FILENAME) String filename
    ) throws DocumentServiceException {
        return downloadProjectDocument(projectCode, bridgehead, filename, DocumentType.OTHERS);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_OTHER_DOCUMENTS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_OTHER_DOCUMENTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchOtherDocuments(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead
    ) {
        return convertToResponseEntity(() -> documentService.fetchOtherDocuments(projectCode, Optional.ofNullable(bridgehead)));
    }

    private ResponseEntity<Resource> downloadProjectDocument(String projectCode, String bridgehead, DocumentType documentType) throws DocumentServiceException {
        return downloadProjectDocument(this.documentService.fetchLastDocumentOfThisType(projectCode, Optional.ofNullable(bridgehead), documentType));
    }

    private ResponseEntity<Boolean> existsProjectDocument(String projectCode, String bridgehead, DocumentType documentType) throws DocumentServiceException {
        return convertToResponseEntity(() -> this.documentService.fetchLastDocumentOfThisType(projectCode, Optional.ofNullable(bridgehead), documentType).isPresent());
    }


    private ResponseEntity<Resource> downloadProjectDocument(String projectCode, String bridgehead, String filename, DocumentType allowedType) throws DocumentServiceException {
        Optional<ProjectDocument> projectDocument = this.documentService.fetchProjectDocument(projectCode, Optional.ofNullable(bridgehead), filename);
        return (projectDocument.isPresent() && projectDocument.get().getDocumentType() != allowedType) ?
                createMethodNotAllowedResponse("Requested document is not of allowed type: " + allowedType) :
                downloadProjectDocument(projectDocument);
    }

    private ResponseEntity<Resource> createMethodNotAllowedResponse(String errorMessage) {
        ByteArrayResource errorResource = new ByteArrayResource(errorMessage.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(errorResource);
    }

    private ResponseEntity<Resource> downloadProjectDocument(Optional<ProjectDocument> projectDocument) throws DocumentServiceException {
        return (projectDocument.isEmpty()) ? ResponseEntity.notFound().build() :
                downloadDocument(encodeFilename(projectDocument.get().getOriginalFilename()), Path.of(projectDocument.get().getFilePath()));
    }

    private ResponseEntity<Resource> downloadDocument(String filename, Path filePath) throws DocumentServiceException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(filePath.toFile().length())
                .body(fetchResource(filePath));
    }

    private String encodeFilename(String filename) {
        try {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return filename;
        }
    }

    private ByteArrayResource fetchResource(Path filePath) throws DocumentServiceException {
        try {
            return new ByteArrayResource(Files.readAllBytes(filePath));
        } catch (IOException e) {
            throw new DocumentServiceException(e);
        }
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL},
            queryStates = {QueryState.CREATED, QueryState.ERROR, QueryState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.SAVE_QUERY_IN_BRIDGEHEAD_ACTION)
    @PostMapping(value = ProjectManagerConst.SAVE_QUERY_IN_BRIDGEHEAD)
    public ResponseEntity<String> saveQueryInBridgehead(
            @NotEmpty @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @NotEmpty @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.projectBridgeheadService.scheduleSendQueryToBridgehead(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL},
            queryStates = {QueryState.CREATED, QueryState.ERROR, QueryState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD_ACTION)
    @PostMapping(value = ProjectManagerConst.SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD)
    public ResponseEntity<String> saveAndExecuteQueryInBridgehead(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.projectBridgeheadService.scheduleSendQueryToBridgeheadAndExecute(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL},
            queryStates = {QueryState.FINISHED})
    @ProjectConstraints(projectTypes = {ProjectType.RESEARCH_ENVIRONMENT, ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.SEND_EXPORT_FILES_TO_RESEARCH_ENVIRONMENT_ACTION)
    @PostMapping(value = ProjectManagerConst.SEND_EXPORT_FILES_TO_RESEARCH_ENVIRONMENT)
    public ResponseEntity<String> sendExportFilesToResearchEnvironment(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> exporterService.transferFileToResearchEnvironment(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL},
            queryStates = {QueryState.FINISHED})
    @ProjectConstraints(projectTypes = {ProjectType.RESEARCH_ENVIRONMENT, ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.ARE_EXPORT_FILES_TRANSFERRED_TO_RESEARCH_ENVIRONMENT_ACTION)
    @PostMapping(value = ProjectManagerConst.ARE_EXPORT_FILES_TRANSFERRED_TO_RESEARCH_ENVIRONMENT)
    public ResponseEntity<String> isExportFileTransferredToResearchEnvironment(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> exporterService.isExportFileTransferredToResearchEnvironment(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.TOKEN_MANAGER_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_AUTHENTICATION_SCRIPT_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_AUTHENTICATION_SCRIPT)
    public ResponseEntity<Resource> fetchTokenScript(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        ProjectManagerConst.AUTHENTICATION_SCRIPT_FILENAME_PREFIX + projectCode + ProjectManagerConst.AUTHENTICATION_SCRIPT_FILENAME_SUFFIX + "\"")
                .body(this.dataShieldTokenManagerService.fetchAuthenticationScript(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.TOKEN_MANAGER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_DATASHIELD_STATUS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_DATASHIELD_STATUS)
    public ResponseEntity<String> fetchOpalStatus(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.dataShieldTokenManagerService.fetchProjectStatus(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.TOKEN_MANAGER_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_AUTHENTICATION_SCRIPT_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_AUTHENTICATION_SCRIPT)
    public ResponseEntity<String> existsTokenScript(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.dataShieldTokenManagerService.existsAuthenticationScript(projectCode, bridgehead));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.NOTIFICATIONS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.NOTIFICATIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_NOTIFICATIONS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_NOTIFICATIONS)
    public ResponseEntity<String> fetchNotifications(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE, required = false) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.notificationService.fetchUserVisibleNotifications(Optional.ofNullable(projectCode), Optional.ofNullable(bridgehead), () -> projectService.fetchAllUserVisibleProjects()));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.NOTIFICATIONS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.NOTIFICATIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_NOTIFICATION_AS_READ_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_NOTIFICATION_AS_READ)
    public ResponseEntity<String> setNotificationAsRead(
            @RequestParam(name = ProjectManagerConst.NOTIFICATION_ID) Long notificationId
    ) {
        return convertToResponseEntity(() -> this.notificationService.setNotificationAsRead(notificationId));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER, OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_ALL_REGISTERED_BRIDGEHEADS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_ALL_REGISTERED_BRIDGEHEADS)
    public ResponseEntity<Resource> fetchAllRegisteredBridgeheads() {
        return convertToResponseEntity(() -> bridgeheadConfiguration.getRegisteredBridgeheads().stream().map(dtoFactory::convertToBridgehead).collect(Collectors.toSet()));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_USERS_FOR_AUTOCOMPLETE_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_USERS_FOR_AUTOCOMPLETE)
    public ResponseEntity<String> fetchUsersForAutocomplete(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @RequestParam(name = ProjectManagerConst.PARTIAL_EMAIL) String partialEmail
    ) {
        return convertToResponseEntity(() -> this.userService.fetchUsersForAutocomplete(projectCode, partialEmail, bridgehead));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_USERS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_USERS)
    public ResponseEntity<String> fetchProjectUsers(
            // Project Code Required for state constraints
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.userService.fetchProjectUsers(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXIST_INVITED_USERS_ACTION)
    @GetMapping(value = ProjectManagerConst.EXIST_INVITED_USERS)
    public ResponseEntity<String> existInvitedUsers(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.userService.existInvatedUsers(projectCode, bridgehead));
    }


    private ResponseEntity convertToResponseEntity(RunnableWithException runnable) {
        try {
            runnable.run();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return createInternalServerError(e);
        }
    }

    private <T> ResponseEntity convertToResponseEntity(SupplierWithException<T> supplier) {
        try {
            T result = supplier.get();
            if (result == null) {
                return ResponseEntity.notFound().build();
            } else if (result instanceof ResponseEntity) {
                return (ResponseEntity) result;
            } else {
                return ResponseEntity.ok(objectMapper.writeValueAsString(result));
            }
        } catch (Exception e) {
            return createInternalServerError(e);
        }
    }

    private ResponseEntity createInternalServerError(Exception e) {
        return ResponseEntity.internalServerError().body(ExceptionUtils.getStackTrace(e));
    }

    private interface RunnableWithException {
        void run() throws Exception;
    }

    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

}
