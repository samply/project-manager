package de.samply.app.controller;

import de.samply.annotations.*;
import de.samply.app.ProjectManagerConst;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheResource;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.email.EmailRecipientType;
import de.samply.email.EmailTemplateType;
import de.samply.project.*;
import de.samply.project.state.ProjectState;
import de.samply.user.DtoUserService;
import de.samply.user.UserService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * ProjectBridgeheadUser (a user of a ProjectBridgehead, with ProjectCoder): set the users per stage,
 * their decisions on script, results and analysis, and their project roles.
 */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class ProjectBridgeheadUserController extends AbstractController {

    private final UserService userService;
    private final DtoUserService dtoUserService;

    public ProjectBridgeheadUserController(UserService userService,
                                           DtoUserService dtoUserService) {
        this.userService = userService;
        this.dtoUserService = dtoUserService;
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_USERS_ACTION)
    @CacheCategory(CacheResource.USER_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_USERS)
    public ResponseEntity fetchProjectUsers(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> this.dtoUserService.fetchProjectUsers(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN,
            ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.EXIST_INVITED_USERS_ACTION)
    @CacheCategory(CacheResource.USER_DATA)
    @GetMapping(value = ProjectManagerConst.EXIST_INVITED_USERS)
    public ResponseEntity existInvitedUsers(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @SuppressWarnings("unused") @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(() -> this.userService.existInvitedUsers(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.DEVELOPER, ProjectRole.PILOT,
            ProjectRole.FINAL, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.USER_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_ROLES_ACTION)
    @CacheCategory(CacheResource.USER_ROLES)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_ROLES)
    public ResponseEntity fetchProjectRoles(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestParameter(name = ProjectManagerConst.BRIDGEHEAD, required = false) ProjectBridgehead bridgehead
    ) {
        return convertToResponseEntity(
                () -> userService.fetchProjectRoles(project, Optional.ofNullable(bridgehead)));
    }

    @RoleConstraints(projectRoles = {ProjectRole.DEVELOPER, ProjectRole.PILOT})
    @StateConstraints(projectStates = {ProjectState.DEVELOP, ProjectState.PILOT})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD})
    @EmailSender(templateType = EmailTemplateType.SCRIPT_ACCEPTED, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.ALL_DEVELOPERS,
            EmailRecipientType.ALL_PILOTS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ACCEPT_SCRIPT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.REQUEST_CHANGES_IN_PROJECT_ANALYSIS)
    public ResponseEntity requestChangesInProjectAnalysis(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            @Bridgehead @RequestVariable(name = ProjectManagerConst.BRIDGEHEAD) ProjectBridgehead bridgehead,
            // Message is sent per email
            @SuppressWarnings("unused") @Message @RequestVariable(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> userService.requestChangesInProject(project, bridgehead));
    }

}
