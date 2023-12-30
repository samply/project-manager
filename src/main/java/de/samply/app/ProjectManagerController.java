package de.samply.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.annotations.*;
import de.samply.db.model.ProjectDocument;
import de.samply.document.DocumentService;
import de.samply.document.DocumentServiceException;
import de.samply.document.DocumentType;
import de.samply.email.EmailRecipientType;
import de.samply.email.EmailTemplateType;
import de.samply.exporter.ExporterService;
import de.samply.frontend.FrontendService;
import de.samply.project.ProjectType;
import de.samply.project.event.ProjectEventActionsException;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectState;
import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;
import de.samply.query.QueryService;
import de.samply.token.TokenManagerService;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Controller
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
    private final TokenManagerService tokenManagerService;

    public ProjectManagerController(ProjectEventService projectEventService,
                                    FrontendService frontendService,
                                    UserService userService,
                                    QueryService queryService,
                                    DocumentService documentService,
                                    ExporterService exporterService,
                                    TokenManagerService tokenManagerService) {
        this.projectEventService = projectEventService;
        this.frontendService = frontendService;
        this.userService = userService;
        this.queryService = queryService;
        this.documentService = documentService;
        this.exporterService = exporterService;
        this.tokenManagerService = tokenManagerService;
    }

    @GetMapping(value = ProjectManagerConst.INFO)
    public ResponseEntity<String> info() {
        return new ResponseEntity<>(projectVersion, HttpStatus.OK);
    }

    @GetMapping(value = ProjectManagerConst.ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchActions(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE, required = false) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.SITE) String site
    ) {
        return convertToResponseEntity(() ->
                this.frontendService.fetchModuleActionPackage(site, Optional.ofNullable(projectCode), Optional.ofNullable(bridgehead)));
    }


    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP})
    @EmailSender(templateType = EmailTemplateType.INVITATION, recipients = {EmailRecipientType.EMAIL_ANNOTATION})
    @EmailSender(templateType = EmailTemplateType.NEW_PROJECT, recipients = {EmailRecipientType.BRIDGEHEAD_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_DEVELOPER_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_DEVELOPER_USER)
    public ResponseEntity<String> setUserAsDeveloper(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @Email @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgheadUserWithRole(email, projectCode, bridgehead, ProjectRole.DEVELOPER));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.PILOT})
    @EmailSender(templateType = EmailTemplateType.INVITATION, recipients = {EmailRecipientType.EMAIL_ANNOTATION})
    @EmailSender(templateType = EmailTemplateType.NEW_PROJECT, recipients = {EmailRecipientType.BRIDGEHEAD_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_PILOT_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_PILOT_USER)
    public ResponseEntity<String> setUserAsPilot(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @Email @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgheadUserWithRole(email, projectCode, bridgehead, ProjectRole.PILOT));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.INVITATION, recipients = {EmailRecipientType.EMAIL_ANNOTATION})
    @EmailSender(templateType = EmailTemplateType.NEW_PROJECT, recipients = {EmailRecipientType.BRIDGHEAD_ADMINS_WHO_HAVE_NOT_ACCEPTED_NOR_REJECTED_THE_PROJECT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_FINAL_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_FINAL_USER)
    public ResponseEntity<String> setUserAsFinal(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @Email @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgheadUserWithRole(email, projectCode, bridgehead, ProjectRole.FINAL));
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
            @RequestParam(name = ProjectManagerConst.EXPLORER_URL, required = false) String explorerUrl
    ) {
        return convertToResponseEntity(() ->
                this.queryService.createQuery(query, queryFormat, label, description, outputFormat, templateId, humanReadable, explorerUrl));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @PostMapping(value = ProjectManagerConst.CREATE_QUERY_AND_DESIGN_PROJECT)
    public ResponseEntity<String> createQueryAndDesignProject(
            @NotEmpty @RequestBody() String query,
            @NotEmpty @RequestParam(name = ProjectManagerConst.QUERY_FORMAT) QueryFormat queryFormat,
            @NotEmpty @RequestParam(name = ProjectManagerConst.BRIDGEHEADS) String[] bridgeheads,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParam(name = ProjectManagerConst.DESCRIPTION, required = false) String description,
            @RequestParam(name = ProjectManagerConst.OUTPUT_FORMAT, required = false) OutputFormat outputFormat,
            @RequestParam(name = ProjectManagerConst.TEMPLATE_ID, required = false) String templateId,
            @RequestParam(name = ProjectManagerConst.HUMAN_READABLE, required = false) String humanReadable,
            @RequestParam(name = ProjectManagerConst.EXPLORER_URL, required = false) String explorerUrl,
            @RequestParam(name = ProjectManagerConst.PROJECT_TYPE, required = false) ProjectType projectType
    ) throws ProjectEventActionsException {
        String queryCode = this.queryService.createQuery(
                query, queryFormat, label, description, outputFormat, templateId, humanReadable, explorerUrl);
        String projectCode = this.projectEventService.draft(bridgeheads, queryCode, projectType);
        return convertToResponseEntity(() -> this.frontendService.fetchUrl(
                ProjectManagerConst.PROJECT_VIEW_SITE,
                Map.of(ProjectManagerConst.QUERY_CODE, projectCode)
        ));
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

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
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
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.ACCEPT_PROJECT)
    public ResponseEntity<String> acceptProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectEventService.accept(projectCode));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.REJECT_PROJECT)
    public ResponseEntity<String> rejectProject(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode
    ) {
        return convertToResponseEntity(() -> projectEventService.reject(projectCode));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
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

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_PROJECT_DOCUMENT_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_PROJECT_DOCUMENT)
    public ResponseEntity<String> uploadProjectDocument(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.DOCUMENT_TYPE) DocumentType documentType,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label,
            @RequestParam(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(
                projectCode, Optional.ofNullable(bridgehead), document, documentType, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_PROJECT_DOCUMENT_URL_ACTION)
    @PostMapping(value = ProjectManagerConst.ADD_PROJECT_DOCUMENT_URL)
    public ResponseEntity<String> addProjectDocumentUrl(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.DOCUMENT_TYPE) DocumentType documentType,
            @RequestParam(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl,
            @RequestParam(name = ProjectManagerConst.LABEL, required = false) String label
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(
                projectCode, Optional.ofNullable(bridgehead), documentUrl, documentType, Optional.ofNullable(label)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.DOWNLOAD_PROJECT_DOCUMENT_ACTION)
    @GetMapping(value = ProjectManagerConst.DOWNLOAD_PROJECT_DOCUMENT)
    public ResponseEntity<Resource> downloadProjectDocument(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.FILENAME) String filename
    ) throws DocumentServiceException {
        Optional<ProjectDocument> projectDocumentOptional = this.documentService.fetchProjectDocument(projectCode, Optional.ofNullable(bridgehead), filename);
        if (projectDocumentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        projectDocumentOptional.get().setOriginalFilename(encodeFilename(projectDocumentOptional.get().getOriginalFilename()));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + projectDocumentOptional.get().getOriginalFilename());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        Path filePath = Path.of(projectDocumentOptional.get().getFilePath());
        ByteArrayResource resource = fetchResource(filePath);
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(filePath.toFile().length())
                .body(resource);
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
    @StateConstraints(projectStates = {ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.SAVE_QUERY_IN_BRIDGEHEAD_ACTION)
    @PostMapping(value = ProjectManagerConst.SAVE_QUERY_IN_BRIDGEHEAD)
    public ResponseEntity<String> saveQueryInBridgehead(
            @NotEmpty @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @NotEmpty @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.exporterService.sendQueryToBridgehead(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @StateConstraints(projectStates = {ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.EXPORT_MODULE)
    @FrontendAction(action = ProjectManagerConst.SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD_ACTION)
    @PostMapping(value = ProjectManagerConst.SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD)
    public ResponseEntity<String> saveAndExecuteQueryInBridgehead(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.exporterService.sendQueryToBridgeheadAndExecute(projectCode, bridgehead));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.TOKEN_MANAGER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_AUTHENTICATION_SCRIPT_ACTION)
    @PostMapping(value = ProjectManagerConst.FETCH_AUTHENTICATION_SCRIPT)
    public ResponseEntity<String> fetchTokenScript(
            @ProjectCode @RequestParam(name = ProjectManagerConst.PROJECT_CODE) String projectCode,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead
    ) {
        return convertToResponseEntity(() -> this.tokenManagerService.fetchAuthenticationScript(projectCode, bridgehead));
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
