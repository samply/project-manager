package de.samply.app;

import de.samply.annotations.RoleConstraints;
import de.samply.annotations.StateConstraints;
import de.samply.project.ProjectParameters;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class ProjectManagerControllerTest {

    private final ProjectEventService projectEventService;

    public ProjectManagerControllerTest(ProjectEventService projectEventService) {
        this.projectEventService = projectEventService;
    }

    @GetMapping(value = "/testCreateProject")
    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER})
    public ResponseEntity<String> testCreateProject() {
        String[] bridgeheads = {"david-j-develop", "frankfurt", "berlin"};
        ProjectParameters projectParameters = new ProjectParameters("test", bridgeheads);
        this.projectEventService.draft(projectParameters);
        return new ResponseEntity<>("Test 6", HttpStatus.OK);
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

    @GetMapping(value = "/test7")
    @StateConstraints(projectStates = {ProjectState.DRAFT})
    public ResponseEntity<String> test7() {
        return new ResponseEntity<>("Test 7", HttpStatus.OK);
    }

    @GetMapping(value = "/test8")
    @StateConstraints(projectStates = {ProjectState.CREATED})
    public ResponseEntity<String> test8() {
        return new ResponseEntity<>("Test 8", HttpStatus.OK);
    }

    @GetMapping(value = "/test9")
    @StateConstraints(projectBridgeheadStates = {ProjectBridgeheadState.CREATED})
    public ResponseEntity<String> test9() {
        return new ResponseEntity<>("Test 9", HttpStatus.OK);
    }

    @GetMapping(value = "/test10")
    @StateConstraints(projectBridgeheadStates = {ProjectBridgeheadState.ACCEPTED})
    public ResponseEntity<String> test10() {
        return new ResponseEntity<>("Test 10", HttpStatus.OK);
    }

}
