package de.samply.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.annotations.RoleConstraints;
import de.samply.project.ProjectParameters;
import de.samply.project.event.ProjectEventService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.ProjectVersion;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.function.Function;
import java.util.function.Supplier;

@Controller
public class ProjectManagerController {

    private final String projectVersion = ProjectVersion.getProjectVersion();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    private final ProjectEventService projectEventService;

    public ProjectManagerController(ProjectEventService projectEventService) {
        this.projectEventService = projectEventService;
    }

    @GetMapping(value = ProjectManagerConst.INFO)
    public ResponseEntity<String> info() {
        return new ResponseEntity<>(projectVersion, HttpStatus.OK);
    }

    @GetMapping(value = "/test0")
    @RoleConstraints(organisationRoles = {OrganisationRole.BRIDGEHEAD_ADMIN})
    public ResponseEntity<String> test0() {
        return new ResponseEntity<>("Test 0", HttpStatus.OK);
    }

    @GetMapping(value = "/test1")
    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    public ResponseEntity<String> test1() {
        return new ResponseEntity<>("Test 1", HttpStatus.OK);
    }

    @GetMapping(value = "/test2")
    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    public ResponseEntity<String> test2() {
        return new ResponseEntity<>("Test 2", HttpStatus.OK);
    }

    @GetMapping(value = "/test3")
    @RoleConstraints(projectRoles = {ProjectRole.PROJECT_MANAGER_ADMIN})
    public ResponseEntity<String> test3() {
        return new ResponseEntity<>("Test 3", HttpStatus.OK);
    }

    @GetMapping(value = "/test4")
    @RoleConstraints(projectRoles = {ProjectRole.BRIDGEHEAD_ADMIN})
    public ResponseEntity<String> test4() {
        return new ResponseEntity<>("Test 4", HttpStatus.OK);
    }

    @GetMapping(value = "/test5")
    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    public ResponseEntity<String> test5() {
        return new ResponseEntity<>("Test 5", HttpStatus.OK);
    }

    @GetMapping(value = "/test6")
    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER})
    public ResponseEntity<String> test6() {
        return new ResponseEntity<>("Test 6", HttpStatus.OK);
    }

    @GetMapping(value = "/testCreateProject")
    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    public ResponseEntity<String> testCreateProject() {
        String[] bridgeheads = {"david-j-develop", "frankfurt", "berlin"};
        ProjectParameters projectParameters = new ProjectParameters("test", bridgeheads);
        this.projectEventService.draft(projectParameters);
        return new ResponseEntity<>("Test 6", HttpStatus.OK);
    }


    @GetMapping(value = ProjectManagerConst.ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchActions(
            //        @RequestParam(name = ProjectManagerConst.QUERY_EXECUTION_ID) Long queryExecutionId
    ) {

        //TODO
        return ResponseEntity.ok("TODO: Return actions");
    }

    private <T, R> ResponseEntity convertToResponseEntity(T input, Function<T, R> function) {
        return convertToResponseEntity(() -> function.apply(input));
    }

    private <T> ResponseEntity convertToResponseEntity(Supplier<T> supplier) {
        try {
            T result = supplier.get();
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
