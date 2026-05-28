package de.samply.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.annotations.*;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.coder.CoderService;
import de.samply.datashield.DataShieldTokenManagerService;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectDocument;
import de.samply.document.DocumentService;
import de.samply.document.DocumentServiceException;
import de.samply.document.DocumentType;
import de.samply.document.DtoDocumentService;
import de.samply.email.EmailRecipientType;
import de.samply.email.EmailService;
import de.samply.email.EmailTemplateType;
import de.samply.exporter.ExporterService;
import de.samply.form.DtoFormService;
import de.samply.form.FormService;
import de.samply.form.template.FormTemplateService;
import de.samply.frontend.FrontendService;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.notification.DtoNotificationService;
import de.samply.notification.NotificationService;
import de.samply.project.*;
import de.samply.project.event.ProjectEventActionsException;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.project.state.UserProjectState;
import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;
import de.samply.query.QueryService;
import de.samply.query.QueryState;
import de.samply.user.DtoUserService;
import de.samply.user.UserService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.ProjectVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Suppress raw types warning:
// This controller intentionally returns raw ResponseEntity to avoid
// excessive generic duplication across many endpoints.

// Suppress unused parameter warning:
// Parameters such as bridgehead and project are not always used
// directly in the method body but are required for validation and
// authorization in the AOP layer.
@SuppressWarnings({"rawtypes"})
@RestController
@Slf4j
public class ProjectManagerController {

    private final String projectVersion = ProjectVersion.getProjectVersion();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(
                    SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    private final ProjectEventService projectEventService;
    private final FrontendService frontendService;
    private final UserService userService;
    private final DtoUserService dtoUserService;
    private final QueryService queryService;
    private final DocumentService documentService;
    private final DtoDocumentService dtoDocumentService;
    private final ExporterService exporterService;
    private final DataShieldTokenManagerService dataShieldTokenManagerService;
    private final ProjectService projectService;
    private final DtoProjectService dtoProjectService;
    private final ProjectBridgeheadService projectBridgeheadService;
    private final DtoProjectBridgeheadService dtoProjectBridgeheadService;
    private final NotificationService notificationService;
    private final DtoNotificationService dtoNotificationService;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final ProjectConfigurations frontendProjectConfigurations;
    private final DtoFactory dtoFactory;
    private final EmailService emailService;
    private final CoderService coderService;
    private final FormService formService;
    private final DtoFormService dtoFormService;
    private final FormTemplateService formTemplateService;

    public ProjectManagerController(ProjectEventService projectEventService,
                                    FrontendService frontendService,
                                    UserService userService,
                                    DtoUserService dtoUserService,
                                    QueryService queryService,
                                    DocumentService documentService,
                                    DtoDocumentService dtoDocumentService,
                                    ExporterService exporterService,
                                    DataShieldTokenManagerService dataShieldTokenManagerService,
                                    ProjectService projectService,
                                    DtoProjectService dtoProjectService,
                                    ProjectBridgeheadService projectBridgeheadService,
                                    DtoProjectBridgeheadService dtoProjectBridgeheadService,
                                    NotificationService notificationService,
                                    DtoNotificationService dtoNotificationService,
                                    BridgeheadConfiguration bridgeheadConfiguration,
                                    ProjectConfigurations frontendProjectConfigurations,
                                    DtoFactory dtoFactory,
                                    EmailService emailService,
                                    CoderService coderService,
                                    FormService formService,
                                    DtoFormService dtoFormService,
                                    FormTemplateService formTemplateService) {
        this.projectEventService = projectEventService;
        this.frontendService = frontendService;
        this.userService = userService;
        this.dtoUserService = dtoUserService;
        this.queryService = queryService;
        this.documentService = documentService;
        this.dtoDocumentService = dtoDocumentService;
        this.exporterService = exporterService;
        this.dataShieldTokenManagerService = dataShieldTokenManagerService;
        this.projectService = projectService;
        this.dtoProjectService = dtoProjectService;
        this.projectBridgeheadService = projectBridgeheadService;
        this.dtoProjectBridgeheadService = dtoProjectBridgeheadService;
        this.notificationService = notificationService;
        this.dtoNotificationService = dtoNotificationService;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.frontendProjectConfigurations = frontendProjectConfigurations;
        this.dtoFactory = dtoFactory;
        this.emailService = emailService;
        this.coderService = coderService;
        this.formService = formService;
        this.dtoFormService = dtoFormService;
        this.formTemplateService = formTemplateService;
    }

    @GetMapping(value = ProjectManagerConst.INFO)
    public ResponseEntity info() {
        return new ResponseEntity<>(projectVersion, HttpStatus.OK);
    }

    @GetMapping(value = ProjectManagerConst.TEST)
    public ResponseEntity test() {
        return new ResponseEntity<>(ProjectManagerConst.THIS_IS_A_TEST, HttpStatus.OK);
    }

    @GetMapping(value = ProjectManagerConst.ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchActions(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language,
            @RequestParameter(name = ProjectManagerConst.SITE) String site
    ) {
        return convertToResponseEntity(() ->
                this.frontendService.fetchModuleActionPackage(site, Optional.ofNullable(project),
                        Optional.ofNullable(bridgehead), Optional.ofNullable(language), true));
    }

    @GetMapping(value = ProjectManagerConst.ALL_ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchAllActions(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language,
            @RequestParameter(name = ProjectManagerConst.SITE) String site
    ) {
        return convertToResponseEntity(() ->
                this.frontendService.fetchModuleActionPackage(site, Optional.ofNullable(project),
                        Optional.ofNullable(bridgehead), Optional.ofNullable(language), false));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.PROJECTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECTS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchProjects(
            @RequestParameter(name = ProjectManagerConst.PROJECT_STATE, required = false) ProjectState projectState,
            @RequestParameter(name = ProjectManagerConst.ARCHIVED, required = false) Boolean archived,
            @RequestParameter(name = ProjectManagerConst.LAST_MODIFIED_DESC, required = false, defaultValue = "true") boolean modifiedDescendant,
            @RequestParameter(name = ProjectManagerConst.PAGE) int page,
            @RequestParameter(name = ProjectManagerConst.PAGE_SIZE) int pageSize
    ) {
        return convertToResponseEntity(() -> dtoProjectService.fetchUserVisibleProjects(
                Optional.ofNullable(projectState), Optional.ofNullable(archived), page, pageSize,
                modifiedDescendant));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchProject(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // Bridgehead required for role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> dtoProjectService.fetchDtoProject(project.getCode()));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_STATES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_STATES, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchProjectStates(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project
    ) {
        return convertToResponseEntity(
                () -> projectEventService.fetchAllProjectEvents(Optional.ofNullable(project)));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_VISIBLE_PROJECT_BRIDGEHEADS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_VISIBLE_PROJECT_BRIDGEHEADS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchVisibleProjectsBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(
                () -> dtoProjectBridgeheadService.fetchUserVisibleProjectBridgeheads(project));
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEADS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEADS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchProjectsBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(
                () -> dtoProjectBridgeheadService.fetchProjectBridgeheads(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT})
    @EmailSender(templateType = EmailTemplateType.INVITATION, recipients = {
            EmailRecipientType.EMAIL_ANNOTATION})
    @EmailSender(templateType = EmailTemplateType.REQUEST_TECHNICAL_APPROVAL, recipients = {
            EmailRecipientType.BRIDGEHEAD_ADMIN})
    //TODO: Send email to PM-ADMIN, that there was a problem with the operation
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_DEVELOPER_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_DEVELOPER_USER)
    public ResponseEntity setUserAsDeveloper(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @Email @RequestVariable(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgeheadUserWithRoleAndGenerateTokensIfDataShield(email,
                        project, bridgehead, ProjectRole.DEVELOPER));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT})
    @EmailSender(templateType = EmailTemplateType.INVITATION, recipients = {
            EmailRecipientType.EMAIL_ANNOTATION})
    @EmailSender(templateType = EmailTemplateType.REQUEST_TECHNICAL_APPROVAL, recipients = {
            EmailRecipientType.BRIDGEHEAD_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_PILOT_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_PILOT_USER)
    public ResponseEntity setUserAsPilot(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @Email @RequestVariable(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgeheadUserWithRoleAndGenerateTokensIfDataShield(email,
                        project, bridgehead, ProjectRole.PILOT));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.INVITATION, recipients = {
            EmailRecipientType.EMAIL_ANNOTATION})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_FINAL_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_FINAL_USER)
    public ResponseEntity setUserAsFinal(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @Email @RequestVariable(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgeheadUserWithRoleAndGenerateTokensIfDataShield(email,
                        project, bridgehead, ProjectRole.FINAL));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @PostMapping(value = ProjectManagerConst.CREATE_QUERY)
    public ResponseEntity createProjectQuery(
            @RequestVariable(name = ProjectManagerConst.QUERY, notEmpty = true) String query,
            @RequestVariable(name = ProjectManagerConst.QUERY_FORMAT, notEmpty = true) QueryFormat queryFormat,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestVariable(name = ProjectManagerConst.DESCRIPTION, required = false) String description,
            @RequestVariable(name = ProjectManagerConst.OUTPUT_FORMAT, required = false) OutputFormat outputFormat,
            @RequestVariable(name = ProjectManagerConst.TEMPLATE_ID, required = false) String templateId,
            @RequestVariable(name = ProjectManagerConst.HUMAN_READABLE, required = false) String humanReadable,
            @RequestVariable(name = ProjectManagerConst.REDIRECT_EXPLORER_URL, required = false) String explorerUrl,
            @RequestVariable(name = ProjectManagerConst.QUERY_CONTEXT, required = false) String queryContext,
            @RequestVariable(name = ProjectManagerConst.QUERY_DETAILS, required = false) String queryDetails,
            @RequestVariable(name = ProjectManagerConst.PROJECT_TYPE, required = false) ProjectType projectType
    ) {
        return convertToResponseEntity(() ->
                this.queryService.createQuery(query, queryFormat, label, description,
                        outputFormat, templateId, projectType, humanReadable, explorerUrl, queryContext,
                        queryDetails));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @PostMapping(value = ProjectManagerConst.CREATE_QUERY_AND_DESIGN_PROJECT)
    public ResponseEntity createQueryAndDesignProject(
            @RequestVariable(name = ProjectManagerConst.QUERY, notEmpty = true) String query,
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
        if (areBridgeheadsOrExplorerIdsEmpty(bridgeheads, explorerIds)) {
            return ResponseEntity.badRequest().body("Bridgeheads or explorer ids cannot be empty");
        }
        String[] tempBridgeheads = (explorerIds != null && explorerIds.length > 0) ?
                Arrays.stream(explorerIds)
                        .map(bridgeheadConfiguration::getBridgeheadForExplorerId)
                        .flatMap(Optional::stream)
                        .toArray(String[]::new) : bridgeheads;
        String queryCode = this.queryService.createQuery(
                query, queryFormat, label, description, outputFormat, templateId,
                projectType, humanReadable, explorerUrl, queryContext, queryDetails);
        String projectCode = this.projectEventService.draft(tempBridgeheads, queryCode);
        this.queryService.addProjectCodeToExporterUrl(queryCode, projectCode);
        return convertToResponseEntity(() -> this.frontendService.fetchExplorerRedirectUri(
                ProjectManagerConst.PROJECT_VIEW_SITE,
                Map.of(ProjectManagerConst.PROJECT_CODE, projectCode)
        ));
    }

    private boolean areBridgeheadsOrExplorerIdsEmpty(String[] bridgeheads, String[] explorerIds) {
        return (bridgeheads == null || bridgeheads.length == 0) && (explorerIds == null
                || explorerIds.length == 0);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.EDIT_PROJECT_ACTION)
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
        if (bridgeheads != null) {
            String[] tempBridgeheads = (explorerIds != null && explorerIds.length > 0) ?
                    Arrays.stream(explorerIds)
                            .map(bridgeheadConfiguration::getBridgeheadForExplorerId)
                            .flatMap(Optional::stream)
                            .toArray(String[]::new) : bridgeheads;
            projectService.updateBridgeheads(project, tempBridgeheads);
        }
        queryService.editQuery(project,
                (query != null && !query.trim().isEmpty() && !query.equals("{}")) ? query :
                        null, queryFormat, label, description, outputFormat, templateId, projectType,
                humanReadable,
                explorerUrl, queryContext, cohortDefinition, queryDetails);
        return convertToResponseEntity(() -> this.frontendService.fetchExplorerRedirectUri(
                ProjectManagerConst.PROJECT_VIEW_SITE,
                Map.of(ProjectManagerConst.PROJECT_CODE, project.getCode())
        ));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.REMOVE_PROJECT_OUTPUT_ACTION)
    @DeleteMapping(value = ProjectManagerConst.REMOVE_PROJECT_OUTPUT)
    public ResponseEntity removeOutput(
            @RequestVariable(name = ProjectManagerConst.PROJECT_TYPE) ProjectType projectType,
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> this.queryService.removeOutput(project, projectType));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.DEVELOPER, ProjectRole.FINAL, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_FORM_TITLES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_FORM_TITLES)
    public ResponseEntity fetchProjectFormTitles(
            // ProjectCode code needed for role constraints
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language
    ) {
        return convertToResponseEntity(
                () -> dtoFormService.fetchProjectFormTitles(Optional.ofNullable(language)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.DEVELOPER, ProjectRole.FINAL, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_FORM_FIELDS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_FORM_FIELDS)
    public ResponseEntity fetchProjectFormFields(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language,
            @RequestParameter(name = ProjectManagerConst.FORM_TITLE, required = false) String formTitle
    ) {
        return convertToResponseEntity(
                () -> dtoFormService.fetchProjectFormFields(Optional.ofNullable(formTitle), project,
                        Optional.ofNullable(language)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.DEVELOPER, ProjectRole.FINAL, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_SELECTED_PROJECT_FORMS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_SELECTED_PROJECT_FORMS)
    public ResponseEntity fetchSelectedProjectForms(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language
    ) {
        return convertToResponseEntity(
                () -> dtoFormService.fetchSelectedForms(project, Optional.ofNullable(language)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_SELECTED_PROJECT_FORM_ACTION)
    @PostMapping(value = ProjectManagerConst.ADD_SELECTED_PROJECT_FORM)
    public ResponseEntity addSelectedProjectForm(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestVariable(name = ProjectManagerConst.FORM_TITLE) String formTitle
    ) {
        return convertToResponseEntity(() -> formService.addSelectedForm(project, formTitle));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.REMOVE_SELECTED_PROJECT_FORM_ACTION)
    @DeleteMapping(value = ProjectManagerConst.REMOVE_SELECTED_PROJECT_FORM)
    public ResponseEntity removeSelectedProjectForm(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.FORM_TITLE) String formTitle
    ) {
        return convertToResponseEntity(() -> formService.removeSelectedForm(project, formTitle));
    }


    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.EDIT_PROJECT_FORM_FIELDS_ACTION)
    @PutMapping(value = ProjectManagerConst.EDIT_PROJECT_FORM_FIELDS)
    public ResponseEntity editProjectFormValues(
            // ProjectCode code needed for role constraints
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestVariable(name = ProjectManagerConst.FORM_FIELDS) FormField[] formFields
    ) {
        return convertToResponseEntity(
                () -> formService.editProjectFormFieldValues(Optional.ofNullable(formFields), project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_FORM_AS_PDF_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_FORM_AS_PDF)
    public ResponseEntity downloadFormAsPdf(
            // ProjectCode code and bridgehead needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language,
            @RequestParameter(name = ProjectManagerConst.FORM_TEMPLATE) String formTemplate
    ) {
        return convertToResponseEntity(() ->
                ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" +
                                formTemplateService.fetchFormFilename(project, formTemplate))
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(formTemplateService.createFormAsPdf(project, formTemplate,
                                Optional.ofNullable(language)))
        );
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_FORM_TEMPLATES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_FORM_TEMPLATES)
    public ResponseEntity fetchProjectFormTemplates(
            // ProjectCode code and bridgehead needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language
    ) {
        return convertToResponseEntity(
                () -> formTemplateService.fetchTemplates(project, Optional.ofNullable(language)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN,
            ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_BEST_PROJECT_FORM_TEMPLATES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_BEST_PROJECT_FORM_TEMPLATES)
    public ResponseEntity fetchBestProjectFormTemplates(
            // ProjectCode code and bridgehead needed for role constraints
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Language String language
    ) {
        return convertToResponseEntity(
                () -> formTemplateService.fetchBestTemplates(project, Optional.ofNullable(language)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_EXPORTER_TEMPLATES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_EXPORTER_TEMPLATES)
    public ResponseEntity fetchExporterTemplates(
            // ProjectCode code needed for role constraints
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(exporterService::getExporterTemplates);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_QUERY_FORMATS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_QUERY_FORMATS)
    public ResponseEntity fetchQueryFormats(
            // ProjectCode code needed for role constraints
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(QueryFormat::values);
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.NAVIGATION_BAR_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.IS_PROJECT_MANAGER_ADMIN_ACTION)
    @GetMapping(value = ProjectManagerConst.IS_PROJECT_MANAGER_ADMIN)
    public ResponseEntity isProjectManagerAdmin(
    ) {
        return convertToResponseEntity(userService::isProjectManagerAdmin);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_ROLES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_ROLES)
    public ResponseEntity fetchProjectRoles(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> userService.fetchProjectRoles(project, Optional.ofNullable(bridgehead)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_OUTPUT_FORMATS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_OUTPUT_FORMATS)
    public ResponseEntity fetchOutputFormats(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectService.fetchOutputFormats(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_RESEARCH_ENVIRONMENT_URL_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_RESEARCH_ENVIRONMENT_URL)
    public ResponseEntity fetchResearchEnvironmentUrl(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(coderService::getResearchEnvironmentUrl);
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_RESEARCH_ENVIRONMENT_WORKSPACE_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_RESEARCH_ENVIRONMENT_WORKSPACE)
    public ResponseEntity existsUserResearchEnvironmentWorkspace(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> coderService.existsUserResearchEnvironmentWorkspace(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_CONFIGURATIONS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_CONFIGURATIONS)
    public ResponseEntity fetchProjectConfigurations(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(this.frontendProjectConfigurations::getConfig);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_CURRENT_PROJECT_CONFIGURATION_ACTION)
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
    @PutMapping(value = ProjectManagerConst.SET_PROJECT_CONFIGURATION)
    public ResponseEntity setProjectConfiguration(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestVariable(name = ProjectManagerConst.PROJECT_CONFIGURATION) String projectConfigurationName
    ) {
        return convertToResponseEntity(
                () -> this.projectService.setProjectConfiguration(project, projectConfigurationName));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_TYPES_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_TYPES)
    public ResponseEntity fetchProjectTypes(
            // ProjectCode code needed for role constraints
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(ProjectType::values);
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @PostMapping(value = ProjectManagerConst.DESIGN_PROJECT)
    public ResponseEntity designProject(
            @RequestVariable(name = ProjectManagerConst.BRIDGEHEADS) String[] bridgeheads,
            @RequestVariable(name = ProjectManagerConst.QUERY_CODE) String queryCode
    ) {
        return convertToResponseEntity(() -> this.projectEventService.draft(bridgeheads, queryCode));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT})
    @EmailSender(templateType = EmailTemplateType.NEW_PROJECT, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.CREATOR})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.CREATE_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.CREATE_PROJECT)
    public ResponseEntity createProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.create(project));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.APPROVAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_DEVELOP_STAGE_ACTION)
    @PutMapping(value = ProjectManagerConst.START_DEVELOP_STAGE)
    public ResponseEntity startDevelopStage(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.startDevelopStage(project));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_PILOT_STAGE_ACTION)
    @PutMapping(value = ProjectManagerConst.START_PILOT_STAGE)
    public ResponseEntity startPilotStage(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.startPilotStage(project));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.PILOT, ProjectState.APPROVAL})
    @EmailSender(templateType = EmailTemplateType.REQUEST_TECHNICAL_APPROVAL, recipients = {
            EmailRecipientType.BRIDGEHEAD_ADMINS_WHO_HAVE_NOT_ACCEPTED_NOR_REJECTED_THE_PROJECT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_FINAL_STAGE_ACTION)
    @PutMapping(value = ProjectManagerConst.START_FINAL_STAGE)
    public ResponseEntity startFinalStage(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.startFinalStage(project));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.ARCHIVED})
    @EmailSender(templateType = EmailTemplateType.NEW_PROJECT_ACCEPTED, recipients = {
            EmailRecipientType.CREATOR})
    @EmailSender(templateType = EmailTemplateType.NEW_PROJECT_ACCEPTED, recipients = {
            EmailRecipientType.ALL_BRIDGEHEAD_ADMINS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_PROJECT_ACTION)
    @PutMapping(value = ProjectManagerConst.ACCEPT_PROJECT)
    public ResponseEntity acceptProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.accept(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.DEVELOP,
            ProjectState.PILOT, ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.PROJECT_REJECTED, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_ACTION)
    @PutMapping(value = ProjectManagerConst.REJECT_PROJECT)
    public ResponseEntity rejectProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // Message is sent per email
            @SuppressWarnings("unused") @Message @RequestVariable(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> projectEventService.reject(project));
    }

    @RoleConstraints(organisationRoles = OrganisationRole.PROJECT_MANAGER_ADMIN)
    @FrontendSiteModule(site = ProjectManagerConst.CONFIGURATION_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_USER_TO_MAILING_BLACK_LIST_ACTION)
    @PostMapping(value = ProjectManagerConst.ADD_USER_TO_MAILING_BLACK_LIST)
    public ResponseEntity addUserToMailingBlackList(
            @Email @RequestVariable(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() -> userService.updateUserInMailingBlackList(email, true));
    }

    @RoleConstraints(organisationRoles = OrganisationRole.PROJECT_MANAGER_ADMIN)
    @FrontendSiteModule(site = ProjectManagerConst.CONFIGURATION_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.REMOVE_USER_FROM_MAILING_BLACK_LIST_ACTION)
    @DeleteMapping(value = ProjectManagerConst.REMOVE_USER_FROM_MAILING_BLACK_LIST)
    public ResponseEntity removeUserFromMailingBlackList(
            @Email @RequestParameter(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() -> userService.updateUserInMailingBlackList(email, false));
    }

    @RoleConstraints(organisationRoles = OrganisationRole.PROJECT_MANAGER_ADMIN)
    @FrontendSiteModule(site = ProjectManagerConst.CONFIGURATION_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_MAILING_BLACK_LIST_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_MAILING_BLACK_LIST)
    public ResponseEntity fetchMailingBlackList() {
        return convertToResponseEntity(dtoUserService::fetchMailingBlackList);
    }

    @RoleConstraints(organisationRoles = OrganisationRole.PROJECT_MANAGER_ADMIN)
    @FrontendSiteModule(site = ProjectManagerConst.CONFIGURATION_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_USERS_FOR_AUTOCOMPLETE_IN_MAILING_BLACK_LIST_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_USERS_FOR_AUTOCOMPLETE_IN_MAILING_BLACK_LIST)
    public ResponseEntity fetchUsersForAutocompleteInMailingBlackList(
            @RequestParameter(name = ProjectManagerConst.EMAIL, required = false) String partialEmail
    ) {
        return convertToResponseEntity(
                () -> dtoUserService.fetchUsersForAutocompleteInMailingBlackList(partialEmail));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINAL}, projectBridgeheadStates = {
            ProjectBridgeheadState.ACCEPTED})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BRIDGEHEAD_RESULTS_URL_ADDED, recipients = {
            EmailRecipientType.ALL_FINALS, EmailRecipientType.CREATOR})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_PROJECT_BRIDGEHEAD_RESULTS_URL_ACTION)
    @PostMapping(value = ProjectManagerConst.ADD_PROJECT_BRIDGEHEAD_RESULTS_URL)
    public ResponseEntity addProjectBridgeheadResultsUrl(
            @SuppressWarnings("unused") @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.RESULTS_URL) String resultsUrl
    ) {
        return convertToResponseEntity(
                () -> projectBridgeheadService.addResultsUrl(bridgehead, resultsUrl));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_RESULTS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_RESULTS)
    public ResponseEntity fetchProjectResults(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(() -> dtoProjectService.fetchResults(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEAD_RESULTS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEAD_RESULTS)
    public ResponseEntity fetchProjectBridgeheadResults(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> dtoProjectBridgeheadService.fetchResults(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEAD_RESULTS_FOR_OWN_BRIDGEHEAD_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_BRIDGEHEAD_RESULTS_FOR_OWN_BRIDGEHEAD)
    public ResponseEntity fetchProjectBridgeheadResultsForOwnBridgehead(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(() -> Optional.of(
                dtoProjectBridgeheadService.fetchResultsOfOwnBridgehead(bridgehead)));
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
    @PostMapping(value = ProjectManagerConst.ADD_PROJECT_RESULTS_URL)
    public ResponseEntity addProjectResultsUrl(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
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
    @PutMapping(value = ProjectManagerConst.ACCEPT_PROJECT_RESULTS_URL)
    public ResponseEntity acceptProjectResultsUrlByCreator(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
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
    @PutMapping(value = ProjectManagerConst.REJECT_PROJECT_RESULTS_URL)
    public ResponseEntity rejectProjectResultsUrlByCreator(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
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
    @PutMapping(value = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_RESULTS_URL)
    public ResponseEntity requestChangesInProjectResultsUrlByCreator(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> projectService.requestChangesInResultsForCreator(project));
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
    @PutMapping(value = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_BRIDGEHEAD_RESULTS_URL)
    public ResponseEntity requestChangesProjectBridgeheadResultsUrlByCreator(
            @SuppressWarnings("unused") @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> projectBridgeheadService.requestChangesInResultsForCreator(bridgehead));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_RESULTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_EMAIL_MESSAGE_AND_SUBJECT_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_EMAIL_MESSAGE_AND_SUBJECT)
    public ResponseEntity fetchEmailMessageAndSubject(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @Email @RequestParameter(name = ProjectManagerConst.EMAIL) String email,
            @RequestParameter(name = ProjectManagerConst.PROJECT_ROLE) ProjectRole projectRole,
            @RequestParameter(name = ProjectManagerConst.EMAIL_TEMPLATE_TYPE) EmailTemplateType emailTemplateType
    ) {
        return convertOptionalToResponseEntity(
                () -> emailService.createEmailMessageAndSubject(email, Optional.ofNullable(project),
                        Optional.ofNullable(bridgehead), projectRole, emailTemplateType));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT,
            ProjectState.FINAL}, queryStates = {QueryState.FINISHED, QueryState.ERROR})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BRIDGEHEAD_ACCEPTED, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_BRIDGEHEAD_PROJECT_ACTION)
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

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @EmailSender(templateType = EmailTemplateType.SCRIPT_ACCEPTED, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS,
            EmailRecipientType.ALL_PILOTS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_SCRIPT_ACTION)
    @PutMapping(value = ProjectManagerConst.ACCEPT_SCRIPT)
    public ResponseEntity acceptScript(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> userService.acceptProject(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @EmailSender(templateType = EmailTemplateType.SCRIPT_REJECTED, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS,
            EmailRecipientType.ALL_PILOTS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_SCRIPT_ACTION)
    @PutMapping(value = ProjectManagerConst.REJECT_SCRIPT)
    public ResponseEntity rejectScript(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            // Message is sent per email
            @SuppressWarnings("unused") @Message @RequestVariable(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.rejectProject(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @EmailSender(templateType = EmailTemplateType.REQUEST_CHANGES_IN_SCRIPT, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS,
            EmailRecipientType.ALL_PILOTS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REQUEST_SCRIPT_CHANGES_ACTION)
    @PutMapping(value = ProjectManagerConst.REQUEST_SCRIPT_CHANGES)
    public ResponseEntity requestChangesInScript(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            // Message is sent per email
            @SuppressWarnings("unused") @Message @RequestVariable(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.requestChangesInProject(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @EmailSender(templateType = EmailTemplateType.RESULTS_ACCEPTED, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_PROJECT_RESULTS_ACTION)
    @PutMapping(value = ProjectManagerConst.ACCEPT_PROJECT_RESULTS)
    public ResponseEntity acceptProjectResults(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> userService.acceptProject(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT,
            ProjectType.EXPORT})
    @EmailSender(templateType = EmailTemplateType.RESULTS_REJECTED, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_RESULTS_ACTION)
    @PutMapping(value = ProjectManagerConst.REJECT_PROJECT_RESULTS)
    public ResponseEntity rejectProjectResults(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            // Message is sent per email
            @SuppressWarnings("unused") @Message @RequestVariable(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.rejectProject(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.REQUEST_CHANGES_IN_PROJECT, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_ACTION)
    @PutMapping(value = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT)
    public ResponseEntity requestChangesInProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            // Message is sent per email
            @SuppressWarnings("unused") @Message @RequestVariable(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.requestChangesInProject(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.RESEARCH_ENVIRONMENT})
    @EmailSender(templateType = EmailTemplateType.ANALYSIS_ACCEPTED, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS,
            EmailRecipientType.ALL_PILOTS, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_PROJECT_ANALYSIS_ACTION)
    @PutMapping(value = ProjectManagerConst.ACCEPT_PROJECT_ANALYSIS)
    public ResponseEntity acceptProjectAnalysis(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> userService.acceptProject(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.RESEARCH_ENVIRONMENT})
    @EmailSender(templateType = EmailTemplateType.ANALYSIS_REJECTED, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS,
            EmailRecipientType.ALL_PILOTS, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_ANALYSIS_ACTION)
    @PutMapping(value = ProjectManagerConst.REJECT_PROJECT_ANALYSIS)
    public ResponseEntity rejectProjectAnalysis(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            // Message is sent per email
            @SuppressWarnings("unused") @Message @RequestVariable(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.rejectProject(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.RESEARCH_ENVIRONMENT})
    @EmailSender(templateType = EmailTemplateType.REQUEST_CHANGES_IN_PROJECT_ANALYSIS, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS,
            EmailRecipientType.ALL_PILOTS, EmailRecipientType.ALL_FINALS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_ANALYSIS_ACTION)
    @PutMapping(value = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_ANALYSIS)
    public ResponseEntity requestChangesInProjectAnalysis(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            // Message is sent per email
            @SuppressWarnings("unused") @Message @RequestVariable(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.requestChangesInProject(project, bridgehead));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.APPROVAL,
            ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ARCHIVE_PROJECT_ACTION)
    @PutMapping(value = ProjectManagerConst.ARCHIVE_PROJECT)
    public ResponseEntity archiveProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.archive(project));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.FINISHED_PROJECT, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.FINISH_PROJECT_ACTION)
    @PutMapping(value = ProjectManagerConst.FINISH_PROJECT)
    public ResponseEntity finishProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.finish(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_OTHER_DOCUMENT_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_OTHER_DOCUMENT)
    public ResponseEntity uploadOtherDocument(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying developer, pilot, final user or bridgehead admin in role constraints
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.ofNullable(bridgehead), document, DocumentType.OTHERS,
                Optional.ofNullable(label)));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @EmailSender(templateType = EmailTemplateType.NEW_PUBLICATION, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_PUBLICATION_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_PUBLICATION)
    public ResponseEntity uploadPublication(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.PUBLICATION, Optional.ofNullable(label)));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @EmailSender(templateType = EmailTemplateType.NEW_FINAL_REPORT, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_FINAL_REPORT_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_FINAL_REPORT)
    public ResponseEntity uploadFinalReport(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.FINAL_REPORT, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_SCRIPT_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_SCRIPT)
    public ResponseEntity uploadScript(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.SCRIPT, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.APPROVAL,
            ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.NEW_VOTUM, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_VOTUM_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_VOTUM)
    public ResponseEntity uploadVotum(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.ofNullable(bridgehead), document, DocumentType.VOTUM,
                Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW})
    @EmailSender(templateType = EmailTemplateType.NEW_VOTUM, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_DESCRIPTION_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_DESCRIPTION)
    public ResponseEntity uploadDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.DESCRIPTION, Optional.ofNullable(label)));
    }


    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.APPROVAL,
            ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.NEW_VOTUM_FOR_ALL_BRIDGEHEADS, recipients = {
            EmailRecipientType.ALL_BRIDGEHEAD_ADMINS, EmailRecipientType.CREATOR,
            EmailRecipientType.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_VOTUM_FOR_ALL_BRIDGEHEADS_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_VOTUM_FOR_ALL_BRIDGEHEADS)
    public ResponseEntity uploadVotumForAllBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParameter(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                project, Optional.empty(), document, DocumentType.VOTUM, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @EmailSender(templateType = EmailTemplateType.NEW_PUBLICATION, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_PUBLICATION_URL_ACTION)
    @PostMapping(value = ProjectManagerConst.ADD_PUBLICATION_URL)
    public ResponseEntity addPublicationUrl(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(
                project, Optional.empty(), documentUrl, DocumentType.PUBLICATION,
                Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @EmailSender(templateType = EmailTemplateType.NEW_FINAL_REPORT, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_FINAL_REPORT_URL_ACTION)
    @PostMapping(value = ProjectManagerConst.ADD_FINAL_REPORT_URL)
    public ResponseEntity addFinalReportUrl(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(
                project, Optional.empty(), documentUrl, DocumentType.FINAL_REPORT,
                Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_OTHER_DOCUMENT_URL_ACTION)
    @PostMapping(value = ProjectManagerConst.ADD_OTHER_DOCUMENT_URL)
    public ResponseEntity addOtherDocumentUrl(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestVariable(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl,
            @RequestVariable(name = ProjectManagerConst.LABEL, required = false) String label
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(
                project, Optional.ofNullable(bridgehead), documentUrl, DocumentType.OTHERS,
                Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL,
            ProjectState.FINISHED, ProjectState.ARCHIVED, ProjectState.REJECTED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_SCRIPT_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_SCRIPT)
    public ResponseEntity downloadScript(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.empty(), DocumentType.SCRIPT);
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL,
            ProjectState.FINISHED, ProjectState.ARCHIVED, ProjectState.REJECTED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_SCRIPT_DESCRIPTION_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_SCRIPT_DESCRIPTION)
    public ResponseEntity fetchScriptDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(
                () -> this.dtoDocumentService.fetchLastDocumentOfThisTypeForFrontend(project,
                        Optional.empty(), DocumentType.SCRIPT));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL,
            ProjectState.FINISHED, ProjectState.ARCHIVED, ProjectState.REJECTED})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_SCRIPT_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_SCRIPT)
    public ResponseEntity existsScript(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // bridgehead required for identifying developer user or bridgehead admin in role constraints
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.SCRIPT);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.APPROVAL,
            ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_VOTUM_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_VOTUM)
    public ResponseEntity downloadVotum(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.of(bridgehead), Optional.empty(), DocumentType.VOTUM);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_DESCRIPTION_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_DESCRIPTION)
    public ResponseEntity downloadDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.empty(), DocumentType.DESCRIPTION);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.APPROVAL,
            ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_VOTUM_FOR_ALL_BRIDGEHEADS_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_VOTUM_FOR_ALL_BRIDGEHEADS)
    public ResponseEntity downloadVotumForAllBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.empty(), DocumentType.VOTUM);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.APPROVAL,
            ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_VOTUM_DESCRIPTION_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_VOTUM_DESCRIPTION)
    public ResponseEntity fetchVotumDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(
                () -> this.dtoDocumentService.fetchLastDocumentOfThisTypeForFrontend(project,
                        Optional.ofNullable(bridgehead), DocumentType.VOTUM));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.APPROVAL,
            ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_DESCRIPTION_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_DESCRIPTION)
    public ResponseEntity fetchProjectDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(
                () -> this.dtoDocumentService.fetchLastDocumentOfThisTypeForFrontend(project,
                        Optional.empty(), DocumentType.DESCRIPTION));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.APPROVAL,
            ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_VOTUM_FOR_ALL_BRIDGEHEADS_DESCRIPTION_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_VOTUM_FOR_ALL_BRIDGEHEADS_DESCRIPTION)
    public ResponseEntity fetchVotumDescriptionForAllBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(
                () -> this.dtoDocumentService.fetchLastDocumentOfThisTypeForFrontend(project,
                        Optional.empty(), DocumentType.VOTUM));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.APPROVAL,
            ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED,
            ProjectState.ARCHIVED, ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_VOTUM_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_VOTUM)
    public ResponseEntity existsVotum(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.of(bridgehead), DocumentType.VOTUM);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED, ProjectState.ARCHIVED,
            ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_PUBLICATION_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_PUBLICATION)
    public ResponseEntity existsPublication(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.PUBLICATION);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED, ProjectState.ARCHIVED,
            ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_FINAL_REPORT_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_FINAL_REPORT)
    public ResponseEntity existsFinalReport(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.FINAL_REPORT);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_EDITION_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_DESCRIPTION_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_DESCRIPTION)
    public ResponseEntity existsDescription(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.DESCRIPTION);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.APPROVAL,
            ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL, ProjectState.FINISHED,
            ProjectState.ARCHIVED, ProjectState.REJECTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.VOTUM_VIEW_SITE, module = ProjectManagerConst.VOTUM_ACTIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_VOTUM_FOR_ALL_BRIDGEHEADS_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_VOTUM_FOR_ALL_BRIDGEHEADS)
    public ResponseEntity existsVotumForAllBridgeheads(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return existsProjectDocument(project, Optional.empty(), DocumentType.VOTUM);
    }

    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_PUBLICATION_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_PUBLICATION)
    public ResponseEntity downloadPublication(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.FILENAME, required = false) String filename
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.ofNullable(filename), DocumentType.PUBLICATION);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_FINAL_REPORT_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_FINAL_REPORT)
    public ResponseEntity downloadFinalReport(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @RequestParameter(name = ProjectManagerConst.FILENAME, required = false) String filename
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.empty(), Optional.ofNullable(filename), DocumentType.FINAL_REPORT);
    }

    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PUBLICATIONS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PUBLICATIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchPublications(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> dtoDocumentService.fetchPublications(project));
    }

    @StateConstraints(projectStates = {ProjectState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_FINAL_REPORTS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_FINAL_REPORTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchFinalReports(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> dtoDocumentService.fetchFinalReports(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_OTHER_DOCUMENT_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_OTHER_DOCUMENT)
    public ResponseEntity downloadOtherProjectDocument(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.FILENAME) String filename
    ) throws DocumentServiceException {
        return downloadProjectDocument(project, Optional.ofNullable(bridgehead), Optional.of(filename), DocumentType.OTHERS);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_OTHER_DOCUMENTS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_OTHER_DOCUMENTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchOtherDocuments(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> dtoDocumentService.fetchOtherDocuments(project, Optional.ofNullable(bridgehead)));
    }

    private ResponseEntity downloadProjectDocument(Project project,
                                                   Optional<ProjectBridgehead> bridgehead,
                                                   Optional<String> filename,
                                                   DocumentType documentType) throws DocumentServiceException {
        Optional<ProjectDocument> projectDocument = filename.isPresent()
                ? this.documentService.fetchProjectDocument(project, bridgehead, filename.get())
                : this.documentService.fetchLastDocumentOfThisType(project, bridgehead, documentType);

        if (filename.isPresent() && projectDocument.isPresent()
                && projectDocument.get().getDocumentType() != documentType) {
            return createMethodNotAllowedResponse("Requested document is not of allowed type: " + documentType);
        }

        return downloadProjectDocument(projectDocument);
    }

    private ResponseEntity existsProjectDocument(Project project,
                                                 Optional<ProjectBridgehead> bridgehead, DocumentType documentType) {
        return convertToResponseEntity(
                () -> this.documentService.fetchLastDocumentOfThisType(project, bridgehead, documentType)
                        .isPresent());
    }

    private ResponseEntity createMethodNotAllowedResponse(String errorMessage) {
        ByteArrayResource errorResource = new ByteArrayResource(errorMessage.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(errorResource);
    }

    private ResponseEntity downloadProjectDocument(Optional<ProjectDocument> projectDocument)
            throws DocumentServiceException {
        return (projectDocument.isEmpty()) ? ResponseEntity.notFound().build() :
                downloadDocument(encodeFilename(projectDocument.get().getOriginalFilename()),
                        Path.of(projectDocument.get().getFilePath()));
    }

    private ResponseEntity downloadDocument(String filename, Path filePath)
            throws DocumentServiceException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(filePath.toFile().length())
                .body(fetchResource(filePath));
    }

    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8);
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


    // This endpoint is active only when external execution is allowed.
    // By default, it is disabled via spring.profiles.active in application.yaml.
    // To enable it, override the configuration so that the
    // 'external-execution-not-allowed' profile is NOT active
    // (e.g., set SPRING_PROFILES_ACTIVE to an empty value).
    @Profile("!" + ProjectManagerConst.EXTERNAL_EXECUTION_NOT_ALLOWED)
    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL},
            queryStates = {QueryState.CREATED, QueryState.ERROR, QueryState.FINISHED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD_ACTION)
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
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL},
            queryStates = {QueryState.FINISHED})
    @ProjectConstraints(projectTypes = {ProjectType.RESEARCH_ENVIRONMENT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.SEND_EXPORT_FILES_TO_RESEARCH_ENVIRONMENT_ACTION)
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
    @GetMapping(value = ProjectManagerConst.ARE_EXPORT_FILES_TRANSFERRED_TO_RESEARCH_ENVIRONMENT)
    public ResponseEntity isExportFileTransferredToResearchEnvironment(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> exporterService.isExportFileTransferredToResearchEnvironment(project.getCode(),
                        bridgehead.getBridgehead()));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.TOKEN_MANAGER_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_AUTHENTICATION_SCRIPT_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_AUTHENTICATION_SCRIPT)
    public ResponseEntity fetchTokenScript(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        ProjectManagerConst.AUTHENTICATION_SCRIPT_FILENAME_PREFIX + project.getCode()
                        + ProjectManagerConst.AUTHENTICATION_SCRIPT_FILENAME_SUFFIX + "\"")
                .body(this.dataShieldTokenManagerService.fetchAuthenticationScript(project, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL,
            ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.TOKEN_MANAGER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_DATASHIELD_STATUS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_DATASHIELD_STATUS)
    public ResponseEntity fetchDatashieldStatus(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> this.dataShieldTokenManagerService.fetchProjectStatus(project, bridgehead).block());
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.TOKEN_MANAGER_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXISTS_AUTHENTICATION_SCRIPT_ACTION)
    @GetMapping(value = ProjectManagerConst.EXISTS_AUTHENTICATION_SCRIPT)
    public ResponseEntity existsTokenScript(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> this.dataShieldTokenManagerService.existsAuthenticationScript(project, bridgehead));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.NOTIFICATIONS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.NOTIFICATIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_NOTIFICATIONS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_NOTIFICATIONS)
    public ResponseEntity fetchNotifications(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() ->
                this.dtoNotificationService.fetchUserVisibleNotifications(
                        Optional.ofNullable(project),
                        Optional.ofNullable(bridgehead),
                        projectService::fetchAllUserVisibleProjects));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.NOTIFICATIONS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.NOTIFICATIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_NOTIFICATION_AS_READ_ACTION)
    @PutMapping(value = ProjectManagerConst.SET_NOTIFICATION_AS_READ)
    public ResponseEntity setNotificationAsRead(
            @RequestVariable(name = ProjectManagerConst.NOTIFICATION_ID) Long notificationId
    ) {
        return convertToResponseEntity(
                () -> this.notificationService.setNotificationAsRead(notificationId));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_ALL_REGISTERED_BRIDGEHEADS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_ALL_REGISTERED_BRIDGEHEADS)
    public ResponseEntity fetchAllRegisteredBridgeheads() {
        return convertToResponseEntity(() -> bridgeheadConfiguration
                .getRegisteredBridgeheads().stream().map(dtoFactory::convertToBridgehead)
                .collect(Collectors.toSet()));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_USERS_FOR_AUTOCOMPLETE_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_USERS_FOR_AUTOCOMPLETE)
    public ResponseEntity fetchUsersForAutocomplete(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.PARTIAL_EMAIL) String partialEmail
    ) {
        return convertToResponseEntity(
                () -> this.dtoUserService.fetchUsersForAutocomplete(partialEmail, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_USERS_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_USERS)
    public ResponseEntity fetchProjectUsers(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> this.dtoUserService.fetchProjectUsers(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_CURRENT_USER_ACTION)
    @GetMapping(value = ProjectManagerConst.FETCH_CURRENT_USER)
    public ResponseEntity fetchCurrentUser(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(() -> this.dtoUserService.fetchCurrentUser(bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXIST_INVITED_USERS_ACTION)
    @GetMapping(value = ProjectManagerConst.EXIST_INVITED_USERS)
    public ResponseEntity existInvitedUsers(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> this.userService.existInvitedUsers(project));
    }


    private ResponseEntity convertToResponseEntity(RunnableWithException runnable) {
        try {
            runnable.run();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return createInternalServerError(e);
        }
    }

    private <T> ResponseEntity convertOptionalToResponseEntity(
            SupplierWithException<Optional<T>> supplier) {
        try {
            Optional<T> result = supplier.get();
            if (result.isEmpty()) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.ok(objectMapper.writeValueAsString(result.get()));
            }
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
