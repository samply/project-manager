package de.samply.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.annotations.*;
import de.samply.frontend.FrontendService;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.ProjectVersion;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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

    public ProjectManagerController(ProjectEventService projectEventService, FrontendService frontendService) {
        this.projectEventService = projectEventService;
        this.frontendService = frontendService;
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

    @GetMapping(value = "/test0")
    @RoleConstraints(organisationRoles = {OrganisationRole.BRIDGEHEAD_ADMIN})
    @FrontendSiteModule(site = "Site 1", module = "Module 1")
    @FrontendAction(action = "test_0")
    public ResponseEntity<String> test0() {
        return new ResponseEntity<>("Test 0", HttpStatus.OK);
    }

    @GetMapping(value = "/test1")
    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = "Site 1", module = "Module 1")
    @FrontendAction(action = "test_1")
    public ResponseEntity<String> test1() {
        return new ResponseEntity<>("Test 1", HttpStatus.OK);
    }

    @GetMapping(value = "/test2")
    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    @FrontendSiteModule(site = "Site 1", module = "Module 2")
    @FrontendAction(action = "test_2")
    public ResponseEntity<String> test2() {
        return new ResponseEntity<>("Test 2", HttpStatus.OK);
    }

    @GetMapping(value = "/test3")
    @RoleConstraints(projectRoles = {ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendAction(action = "test_3")
    @FrontendSiteModule(site = "Site 1", module = "Module 2")
    public ResponseEntity<String> test3() {
        return new ResponseEntity<>("Test 3", HttpStatus.OK);
    }

    @GetMapping(value = "/test4")
    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    @FrontendSiteModule(site = "Site 1", module = "Module 2")
    @FrontendAction(action = "test_4")
    public ResponseEntity<String> test4() {
        return new ResponseEntity<>("Test 4", HttpStatus.OK);
    }

    @GetMapping(value = "/test5")
    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @FrontendSiteModule(site = "Site 2", module = "Module 1")
    @FrontendAction(action = "test_5")
    public ResponseEntity<String> test5() {
        return new ResponseEntity<>("Test 5", HttpStatus.OK);
    }

    @GetMapping(value = "/test6")
    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER})
    @FrontendSiteModule(site = "Site 2", module = "Module 1")
    @FrontendAction(action = "test_6")
    public ResponseEntity<String> test6() {
        return new ResponseEntity<>("Test 6", HttpStatus.OK);
    }

    @GetMapping(value = "/test7")
    @StateConstraints(projectStates = {ProjectState.DRAFT})
    @FrontendSiteModule(site = "Site 2", module = "Module 2")
    @FrontendAction(action = "test_7")
    public ResponseEntity<String> test7() {
        return new ResponseEntity<>("Test 7", HttpStatus.OK);
    }

    @GetMapping(value = "/test8")
    @StateConstraints(projectStates = {ProjectState.CREATED})
    @FrontendSiteModule(site = "Site 2", module = "Module 2")
    @FrontendAction(action = "test_8")
    public ResponseEntity<String> test8() {
        return new ResponseEntity<>("Test 8", HttpStatus.OK);
    }

    @GetMapping(value = "/test9")
    @StateConstraints(projectBridgeheadStates = {ProjectBridgeheadState.CREATED})
    @FrontendSiteModule(site = "Site 2", module = "Module 3")
    @FrontendAction(action = "test_9")
    public ResponseEntity<String> test9() {
        return new ResponseEntity<>("Test 9", HttpStatus.OK);
    }

    @GetMapping(value = "/test10")
    @StateConstraints(projectBridgeheadStates = {ProjectBridgeheadState.ACCEPTED})
    @FrontendSiteModule(site = "Site 2", module = "Module 3")
    @FrontendAction(action = "test_10")
    public ResponseEntity<String> test10() {
        return new ResponseEntity<>("Test 10", HttpStatus.OK);
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
