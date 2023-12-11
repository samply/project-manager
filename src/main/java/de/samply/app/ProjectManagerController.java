package de.samply.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.annotations.*;
import de.samply.document.DocumentService;
import de.samply.frontend.FrontendService;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryFormat;
import de.samply.query.QueryService;
import de.samply.user.UserService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.ProjectVersion;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Controller
public class ProjectManagerController {

    private final String projectVersion = ProjectVersion.getProjectVersion();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    private final ProjectEventService projectEventService;
    private final FrontendService frontendService;
    private final UserService userService;
    private final QueryService queryService;
    private final DocumentService documentService;

    public ProjectManagerController(ProjectEventService projectEventService,
                                    FrontendService frontendService,
                                    UserService userService,
                                    QueryService queryService,
                                    DocumentService documentService) {
        this.projectEventService = projectEventService;
        this.frontendService = frontendService;
        this.userService = userService;
        this.queryService = queryService;
        this.documentService = documentService;
    }

    @GetMapping(value = ProjectManagerConst.INFO)
    public ResponseEntity<String> info() {
        return new ResponseEntity<>(projectVersion, HttpStatus.OK);
    }


    @GetMapping(value = ProjectManagerConst.ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchActions(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME, required = false) String projectName,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD, required = false) String bridgehead,
            @RequestParam(name = ProjectManagerConst.SITE) String site
    ) {
        return convertToResponseEntity(
                this.frontendService.fetchModuleActionPackage(site, Optional.ofNullable(projectName), Optional.ofNullable(bridgehead)));
    }


    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED, ProjectState.ACCEPTED, ProjectState.DEVELOP})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_DEVELOPER_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_DEVELOPER_USER)
    public ResponseEntity<String> setUserAsDeveloper(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgheadUserWithRole(email, projectName, bridgehead, ProjectRole.DEVELOPER));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_PILOT_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_PILOT_USER)
    public ResponseEntity<String> setUserAsPilot(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgheadUserWithRole(email, projectName, bridgehead, ProjectRole.PILOT));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.ACCEPTED, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_FINAL_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_FINAL_USER)
    public ResponseEntity<String> setUserAsFinal(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() ->
                this.userService.setProjectBridgheadUserWithRole(email, projectName, bridgehead, ProjectRole.FINAL));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.DESIGN_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.DESIGN_PROJECT)
    public ResponseEntity<String> designProject(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName,
            @RequestParam(name = ProjectManagerConst.BRIDGEHEADS) String[] bridgeheads
    ) {
        return convertToResponseEntity(() -> this.projectEventService.draft(projectName, bridgeheads));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.CREATE_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.CREATE_PROJECT)
    public ResponseEntity<String> createProject(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName
    ) {
        return convertToResponseEntity(() -> projectEventService.create(projectName));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.ACCEPTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_DEVELOP_STAGE_ACTION)
    @PostMapping(value = ProjectManagerConst.START_DEVELOP_STAGE)
    public ResponseEntity<String> startDevelopStage(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName
    ) {
        return convertToResponseEntity(() -> projectEventService.startDevelopStage(projectName));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_PILOT_STAGE_ACTION)
    @PostMapping(value = ProjectManagerConst.START_PILOT_STAGE)
    public ResponseEntity<String> startPilotStage(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName
    ) {
        return convertToResponseEntity(() -> projectEventService.startPilotStage(projectName));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.PILOT, ProjectState.ACCEPTED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_FINAL_STAGE_ACTION)
    @PostMapping(value = ProjectManagerConst.START_FINAL_STAGE)
    public ResponseEntity<String> startFinalStage(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName
    ) {
        return convertToResponseEntity(() -> projectEventService.startFinalStage(projectName));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.CREATED, ProjectState.ARCHIVED})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.ACCEPT_PROJECT)
    public ResponseEntity<String> acceptProject(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName
    ) {
        return convertToResponseEntity(() -> projectEventService.accept(projectName));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.REJECT_PROJECT)
    public ResponseEntity<String> rejectProject(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName
    ) {
        return convertToResponseEntity(() -> projectEventService.reject(projectName));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ARCHIVE_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.ARCHIVE_PROJECT)
    public ResponseEntity<String> archiveProject(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName
    ) {
        return convertToResponseEntity(() -> projectEventService.archive(projectName));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.FINISH_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.FINISH_PROJECT)
    public ResponseEntity<String> finishProject(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName
    ) {
        return convertToResponseEntity(() -> projectEventService.finish(projectName));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @PostMapping(value = ProjectManagerConst.CREATE_PROJECT_QUERY)
    public ResponseEntity<String> createProjectQuery(
            @RequestParam(name = ProjectManagerConst.QUERY_FORMAT) QueryFormat queryFormat,
            @RequestBody() String query
    ) {
        if (query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return convertToResponseEntity(() -> this.queryService.createQuery(query, queryFormat));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @PostMapping(value = ProjectManagerConst.CREATE_PROJECT_CQL_DATA_QUERY)
    public ResponseEntity<String> createProjectCqlDataQuery(
            @RequestBody() String query
    ) {
        if (query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return convertToResponseEntity(() -> this.queryService.createQuery(query, QueryFormat.CQL_DATA));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER, OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.UPLOAD_PROJECT_DOCUMENT_ACTION)
    @PostMapping(value = ProjectManagerConst.UPLOAD_PROJECT_DOCUMENT)
    public ResponseEntity<String> uploadProjectDocument(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName,
            @RequestParam(name = ProjectManagerConst.DOCUMENT) MultipartFile document
    ) {
        return convertToResponseEntity(() -> this.documentService.uploadDocument(projectName, document));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER, OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_DOCUMENTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_PROJECT_DOCUMENT_URL_ACTION)
    @PostMapping(value = ProjectManagerConst.ADD_PROJECT_DOCUMENT_URL)
    public ResponseEntity<String> addProjectDocumentUrl(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName,
            @RequestParam(name = ProjectManagerConst.DOCUMENT_URL) String documentUrl
    ) {
        return convertToResponseEntity(() -> this.documentService.addDocumentUrl(projectName, documentUrl));
    }

    private ResponseEntity convertToResponseEntity(RunnableWithException runnable) {
        try {
            runnable.run();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return createInternalServerError(e);
        }
    }

    private <T> ResponseEntity convertToResponseEntity(T result) {
        try {
            return (result != null) ? ResponseEntity.ok(objectMapper.writeValueAsString(result))
                    : ResponseEntity.notFound().build();
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
