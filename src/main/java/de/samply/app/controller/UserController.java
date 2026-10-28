package de.samply.app.controller;

import de.samply.annotations.*;
import de.samply.app.ProjectManagerConst;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheResource;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.project.state.ProjectState;
import de.samply.user.DtoUserService;
import de.samply.user.UserService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * User (and the admin and creator users): current user, organisation roles, user search and the mailing
 * black list.
 */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class UserController extends AbstractController {

    private final UserService userService;
    private final DtoUserService dtoUserService;

    public UserController(UserService userService,
                          DtoUserService dtoUserService) {
        this.userService = userService;
        this.dtoUserService = dtoUserService;
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT, ProjectRole.FINAL})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_CURRENT_USER_ACTION)
    @CacheCategory(CacheResource.USER_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_CURRENT_USER)
    public ResponseEntity fetchCurrentUser(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead
    ) {
        return convertOptionalToResponseEntity(() -> this.dtoUserService.fetchCurrentUser(bridgehead));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_ORGANISATION_ROLES_ACTION)
    @CacheCategory(CacheResource.USER_ROLES)
    @GetMapping(value = ProjectManagerConst.FETCH_ORGANISATION_ROLES)
    public ResponseEntity fetchOrganisationRoles() {
        return convertToResponseEntity(userService::fetchOrganisationRoles);
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.NAVIGATION_BAR_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.IS_PROJECT_MANAGER_ADMIN_ACTION)
    @CacheCategory(CacheResource.USER_ROLES)
    @GetMapping(value = ProjectManagerConst.IS_PROJECT_MANAGER_ADMIN)
    public ResponseEntity isProjectManagerAdmin(
    ) {
        return convertToResponseEntity(userService::isProjectManagerAdmin);
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.PROJECTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_CREATORS_ACTION)
    @CacheCategory(CacheResource.USER_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_CREATORS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchProjectCreators(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project
    ) {
        return convertToResponseEntity(
                () -> dtoUserService.fetchProjectCreators(Optional.ofNullable(project)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_USERS_FOR_AUTOCOMPLETE_ACTION)
    @CacheCategory(CacheResource.USER_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_USERS_FOR_AUTOCOMPLETE)
    public ResponseEntity fetchUsersForAutocomplete(
            @SuppressWarnings("unused") @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            @RequestParameter(name = ProjectManagerConst.PARTIAL_EMAIL) String partialEmail
    ) {
        return convertToResponseEntity(
                () -> this.dtoUserService.fetchUsersForAutocomplete(partialEmail, bridgehead));
    }

    @RoleConstraints(organisationRoles = OrganisationRole.PROJECT_MANAGER_ADMIN)
    @FrontendSiteModule(site = ProjectManagerConst.CONFIGURATION_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_MAILING_BLACK_LIST_ACTION)
    @CacheCategory(CacheResource.USER_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_MAILING_BLACK_LIST)
    public ResponseEntity fetchMailingBlackList() {
        return convertToResponseEntity(dtoUserService::fetchMailingBlackList);
    }

    @RoleConstraints(organisationRoles = OrganisationRole.PROJECT_MANAGER_ADMIN)
    @FrontendSiteModule(site = ProjectManagerConst.CONFIGURATION_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_USERS_FOR_AUTOCOMPLETE_IN_MAILING_BLACK_LIST_ACTION)
    @CacheCategory(CacheResource.USER_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_USERS_FOR_AUTOCOMPLETE_IN_MAILING_BLACK_LIST)
    public ResponseEntity fetchUsersForAutocompleteInMailingBlackList(
            @RequestParameter(name = ProjectManagerConst.EMAIL, required = false) String partialEmail
    ) {
        return convertToResponseEntity(
                () -> dtoUserService.fetchUsersForAutocompleteInMailingBlackList(partialEmail));
    }

    @RoleConstraints(organisationRoles = OrganisationRole.PROJECT_MANAGER_ADMIN)
    @FrontendSiteModule(site = ProjectManagerConst.CONFIGURATION_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.ADD_USER_TO_MAILING_BLACK_LIST_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.ADD_USER_TO_MAILING_BLACK_LIST)
    public ResponseEntity addUserToMailingBlackList(
            @Email @RequestVariable(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() -> userService.updateUserInMailingBlackList(email, true));
    }

    @RoleConstraints(organisationRoles = OrganisationRole.PROJECT_MANAGER_ADMIN)
    @FrontendSiteModule(site = ProjectManagerConst.CONFIGURATION_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.REMOVE_USER_FROM_MAILING_BLACK_LIST_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @DeleteMapping(value = ProjectManagerConst.REMOVE_USER_FROM_MAILING_BLACK_LIST)
    public ResponseEntity removeUserFromMailingBlackList(
            @Email @RequestParameter(name = ProjectManagerConst.EMAIL) String email
    ) {
        return convertToResponseEntity(() -> userService.updateUserInMailingBlackList(email, false));
    }

}
