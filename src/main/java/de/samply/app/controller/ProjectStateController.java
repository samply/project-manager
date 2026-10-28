package de.samply.app.controller;

import de.samply.annotations.*;
import de.samply.app.ProjectManagerConst;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheResource;
import de.samply.db.model.Project;
import de.samply.email.EmailRecipientType;
import de.samply.email.EmailTemplateType;
import de.samply.modules.OptionalModule;
import de.samply.project.*;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectState;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/** The state of a Project: its creation and deletion and the transitions of its state machine. */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class ProjectStateController extends AbstractController {

    private final ProjectEventService projectEventService;
    private final ProjectService projectService;

    public ProjectStateController(ProjectEventService projectEventService,
                                  ProjectService projectService) {
        this.projectEventService = projectEventService;
        this.projectService = projectService;
    }

    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_BRIDGEHEAD_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.PROJECTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_PROJECT_STATES_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_PROJECT_STATES, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchProjectStates(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE, required = false) Project project
    ) {
        return convertToResponseEntity(
                () -> projectEventService.fetchAllProjectEvents(Optional.ofNullable(project)));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.RESEARCHER,
            OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.PROJECTS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_VISIBLE_PROJECT_STATES_ACTION)
    @CacheCategory(CacheResource.REFERENCE_DATA)
    @GetMapping(value = ProjectManagerConst.FETCH_VISIBLE_PROJECT_STATES, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity fetchVisibleProjectStates() {
        return convertToResponseEntity(projectService::fetchVisibleProjectStates);
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT})
    @EmailSender(templateType = EmailTemplateType.NEW_PROJECT, recipients = {
            EmailRecipientType.PROJECT_MANAGER_ADMIN, EmailRecipientType.CREATOR})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.CREATE_PROJECT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PostMapping(value = ProjectManagerConst.CREATE_PROJECT)
    public ResponseEntity createProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.create(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR})
    @StateConstraints(projectStates = {ProjectState.DRAFT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.DELETE_PROJECT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @DeleteMapping(value = ProjectManagerConst.DELETE_PROJECT)
    public ResponseEntity deleteProject(
            @ProjectCode @RequestParameter(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectService.deleteProject(project));
    }

    // Only with ENABLE_LOCAL_APPROVAL=true (disabled by default): otherwise neither offered nor accepted
    @RequiresModule(OptionalModule.LOCAL_APPROVAL)
    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.CENTRAL_APPROVAL})
    @EmailSender(templateType = EmailTemplateType.LOCAL_APPROVAL_STARTED, recipients = {
            EmailRecipientType.BRIDGEHEAD_APPROVAL_CONTACTS, EmailRecipientType.CREATOR,
            EmailRecipientType.ALL_BRIDGEHEAD_ADMINS})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_LOCAL_APPROVAL_STAGE_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.START_LOCAL_APPROVAL_STAGE)
    public ResponseEntity startLocalApprovalStage(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.startLocalApprovalStage(project));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.CENTRAL_APPROVAL})
    @EmailSender(templateType = EmailTemplateType.PROJECT_BACK_TO_REVIEW, recipients = {
            EmailRecipientType.CREATOR})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.BACK_TO_REVIEW_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.BACK_TO_REVIEW)
    public ResponseEntity backToReview(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.backToReview(project));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.CENTRAL_APPROVAL, ProjectState.LOCAL_APPROVAL})
    @ProjectConstraints(projectTypes = {ProjectType.DATASHIELD, ProjectType.RESEARCH_ENVIRONMENT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_DEVELOP_STAGE_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.START_PILOT_STAGE)
    public ResponseEntity startPilotStage(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.startPilotStage(project));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.PILOT, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL})
    @EmailSender(templateType = EmailTemplateType.REQUEST_TECHNICAL_APPROVAL, recipients = {
            EmailRecipientType.BRIDGEHEAD_ADMINS_WHO_HAVE_NOT_ACCEPTED_NOR_REJECTED_THE_PROJECT})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.START_FINAL_STAGE_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.ACCEPT_PROJECT)
    public ResponseEntity acceptProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.accept(project));
    }

    @RoleConstraints(projectRoles = {ProjectRole.CREATOR, ProjectRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.DRAFT, ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @EmailSender(templateType = EmailTemplateType.PROJECT_REJECTED, recipients = {
            EmailRecipientType.PROJECT_ALL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.REJECT_PROJECT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.REJECT_PROJECT)
    public ResponseEntity rejectProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project,
            // Message is sent per email
            @SuppressWarnings("unused") @Message @RequestVariable(name = ProjectManagerConst.MESSAGE, required = false) String message
    ) {
        return convertToResponseEntity(() -> projectEventService.reject(project));
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @StateConstraints(projectStates = {ProjectState.REVIEW, ProjectState.CENTRAL_APPROVAL,
            ProjectState.LOCAL_APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.PROJECT_STATE_MODULE)
    @FrontendAction(action = ProjectManagerConst.ARCHIVE_PROJECT_ACTION)
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.FINISH_PROJECT)
    public ResponseEntity finishProject(
            @ProjectCode @RequestVariable(name = ProjectManagerConst.PROJECT_CODE) Project project
    ) {
        return convertToResponseEntity(() -> projectEventService.finish(project));
    }

}
