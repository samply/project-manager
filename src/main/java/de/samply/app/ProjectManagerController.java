package de.samply.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.annotations.*;
import de.samply.frontend.FrontendService;
import de.samply.project.event.ProjectEventActionsException;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectState;
import de.samply.user.UserService;
import de.samply.user.UserServiceException;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Controller
public class ProjectManagerController {

    private final String projectVersion = ProjectVersion.getProjectVersion();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    private final ProjectEventService projectEventService;
    private final FrontendService frontendService;
    private final UserService userService;

    public ProjectManagerController(ProjectEventService projectEventService, FrontendService frontendService, UserService userService) {
        this.projectEventService = projectEventService;
        this.frontendService = frontendService;
        this.userService = userService;
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
        try {
            this.userService.setProjectBridgheadUserWithRole(email, projectName, bridgehead, ProjectRole.DEVELOPER);
            return ResponseEntity.ok().build();
        } catch (UserServiceException e) {
            return createInternalServerError(e);
        }
    }

    @RoleConstraints(projectRoles = {ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.ACCEPTED, ProjectState.DEVELOP, ProjectState.PILOT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_PILOT_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_PILOT_USER)
    public ResponseEntity<String> setUserAsPilot(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        try {
            this.userService.setProjectBridgheadUserWithRole(email, projectName, bridgehead, ProjectRole.PILOT);
            return ResponseEntity.ok().build();
        } catch (UserServiceException e) {
            return createInternalServerError(e);
        }
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.CREATED, ProjectState.DEVELOP})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.SET_FINAL_USER_ACTION)
    @PostMapping(value = ProjectManagerConst.SET_FINAL_USER)
    public ResponseEntity<String> setUserAsFinal(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName,
            @Bridgehead @RequestParam(name = ProjectManagerConst.BRIDGEHEAD) String bridgehead,
            @RequestParam(name = ProjectManagerConst.EMAIL) String email
    ) {
        try {
            this.userService.setProjectBridgheadUserWithRole(email, projectName, bridgehead, ProjectRole.FINAL);
            return ResponseEntity.ok().build();
        } catch (UserServiceException e) {
            return createInternalServerError(e);
        }
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.DESIGN_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.DESIGN_PROJECT)
    public ResponseEntity<String> designProject(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName,
            @RequestParam(name = ProjectManagerConst.BRIDGEHEADS) String[] bridgeheads
    ) {
        try {
            projectEventService.draft(projectName, bridgeheads);
            return ResponseEntity.ok().build();
        } catch (ProjectEventActionsException e) {
            return createInternalServerError(e);
        }
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.DESIGN_PROJECT_ACTION)
    @PostMapping(value = ProjectManagerConst.DESIGN_PROJECT)
    public ResponseEntity<String> createProject(
            @ProjectName @RequestParam(name = ProjectManagerConst.PROJECT_NAME) String projectName
    ) {
        try {
            projectEventService.create(projectName);
            return ResponseEntity.ok().build();
        } catch (ProjectEventActionsException e) {
            return createInternalServerError(e);
        }
    }

    private <T, R> ResponseEntity convertToResponseEntity(T input, Function<T, R> function) {
        return convertToResponseEntity(() -> function.apply(input));
    }

    private <T> ResponseEntity convertToResponseEntity(Supplier<T> supplier) {
        return convertToResponseEntity(() -> supplier.get());
    }

    private <T> ResponseEntity convertToResponseEntity(T result) {
        try {
            return (result != null) ? ResponseEntity.ok(objectMapper.writeValueAsString(result))
                    : ResponseEntity.notFound().build();
        } catch (JsonProcessingException e) {
            return createInternalServerError(e);
        }
    }


    private ResponseEntity createInternalServerError(Exception e) {
        return ResponseEntity.internalServerError().body(ExceptionUtils.getStackTrace(e));
    }


}
