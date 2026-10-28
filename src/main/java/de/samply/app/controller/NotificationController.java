package de.samply.app.controller;

import de.samply.annotations.*;
import de.samply.app.ProjectManagerConst;
import de.samply.cache.CacheCategory;
import de.samply.cache.CacheResource;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.notification.DtoNotificationService;
import de.samply.notification.NotificationService;
import de.samply.project.*;
import de.samply.user.roles.OrganisationRole;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/** Notification of a Project (read state in NotificationUserAction). */
@SuppressWarnings({"rawtypes"}) // See AbstractController
@RestController
public class NotificationController extends AbstractController {

    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final DtoNotificationService dtoNotificationService;

    public NotificationController(ProjectService projectService,
                                  NotificationService notificationService,
                                  DtoNotificationService dtoNotificationService) {
        this.projectService = projectService;
        this.notificationService = notificationService;
        this.dtoNotificationService = dtoNotificationService;
    }

    @RoleConstraints(organisationRoles = {OrganisationRole.PROJECT_MANAGER_ADMIN})
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_VIEW_SITE, module = ProjectManagerConst.NOTIFICATIONS_MODULE)
    @FrontendSiteModule(site = ProjectManagerConst.PROJECT_DASHBOARD_SITE, module = ProjectManagerConst.NOTIFICATIONS_MODULE)
    @FrontendAction(action = ProjectManagerConst.FETCH_NOTIFICATIONS_ACTION)
    @CacheCategory(CacheResource.NOTIFICATIONS)
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
    @CacheCategory(CacheResource.MUTATION_RESPONSES)
    @PutMapping(value = ProjectManagerConst.SET_NOTIFICATION_AS_READ)
    public ResponseEntity setNotificationAsRead(
            @RequestVariable(name = ProjectManagerConst.NOTIFICATION_ID) Long notificationId
    ) {
        return convertToResponseEntity(
                () -> this.notificationService.setNotificationAsRead(notificationId));
    }

}
