package de.samply.notification;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Notification;
import de.samply.db.model.NotificationUserAction;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.NotificationRepository;
import de.samply.db.repository.NotificationUserActionRepository;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationUserActionRepository notificationUserActionRepository;
    private final SessionUser sessionUser;


    public NotificationService(NotificationRepository notificationRepository,
                               NotificationUserActionRepository notificationUserActionRepository,
                               SessionUser sessionUser) {
        this.notificationRepository = notificationRepository;
        this.notificationUserActionRepository = notificationUserActionRepository;
        this.sessionUser = sessionUser;
    }

    @Async(ProjectManagerConst.ASYNC_NOTIFICATION_EXECUTOR)
    public void createNotification(@NotNull Project project, String bridgehead, String email,
                                   @NotNull OperationType operationType,
                                   @NotNull String details, String error, HttpStatus httpStatus
    ) throws NotificationServiceException {
        Notification notification = new Notification();
        notification.setProject(project);
        notification.setBridgehead(bridgehead);
        notification.setEmail(email);
        notification.setOperationType(operationType);
        notification.setDetails(details);
        notification.setError(error);
        notification.setHttpStatus(httpStatus);
        notificationRepository.save(notification);
    }

    // We use a supplier of ProjectService.fetchAllUserVisibleProjects to remove interdependence
    // between the notification service and the project service.
    protected List<Notification> fetchUserVisibleNotifications(
            Optional<Project> project, Optional<ProjectBridgehead> bridgehead,
            Supplier<List<Project>> allUserVisibleProjectFetcher) throws NotificationServiceException {
        List<Notification> result = new ArrayList<>();
        List<Project> projects = project.map(List::of).orElseGet(allUserVisibleProjectFetcher);
        List<String> bridgeheads = fetchUserVisibleBridgeheads(bridgehead.map(ProjectBridgehead::getBridgehead));
        projects.forEach(tempProject -> {
            if (bridgeheads.isEmpty() && sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.PROJECT_MANAGER_ADMIN)) {
                result.addAll(notificationRepository.findByProjectOrderByTimestampDesc(tempProject));
            } else {
                bridgeheads.forEach(tempBridgehead -> result.addAll(
                        notificationRepository.findByProjectAndBridgeheadOrBridgeheadIsNullOrderByTimestampDesc(tempProject, tempBridgehead)));
            }
        });
        return result;
    }

    private List<String> fetchUserVisibleBridgeheads(Optional<String> requestedBridgehead) {
        if (sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.PROJECT_MANAGER_ADMIN)) {
            return requestedBridgehead.map(List::of).orElseGet(ArrayList::new);
        } else {
            return requestedBridgehead.<List<String>>map(s -> (sessionUser.getBridgeheads().contains(s)) ?
                    List.of(s) : new ArrayList<>()).orElseGet(() -> sessionUser.getBridgeheads().stream().toList());

        }
    }

    @Async(ProjectManagerConst.ASYNC_NOTIFICATION_EXECUTOR)
    public void setNotificationAsRead(@NotNull Long notificationId) {
        NotificationUserAction notificationUserAction = fetchNotificationUserAction(notificationId);
        notificationUserAction.setRead(true);
        notificationUserAction.setModifiedAt(Instant.now());
        notificationUserActionRepository.save(notificationUserAction);
    }

    public NotificationUserAction fetchNotificationUserAction(@NotNull Long notificationId) {
        Optional<Notification> notificationOptional = notificationRepository.findById(notificationId);
        if (notificationOptional.isEmpty()) {
            throw new NotificationServiceException("Notification " + notificationId + " not found");
        }
        return fetchNotificationUserAction(notificationOptional.get());
    }

    public NotificationUserAction fetchNotificationUserAction(@NotNull Notification notification) {
        Optional<NotificationUserAction> notificationUserActionOptional = notificationUserActionRepository.findByNotification(notification);
        NotificationUserAction notificationUserAction;
        if (notificationUserActionOptional.isEmpty()) {
            notificationUserAction = new NotificationUserAction();
            notificationUserAction.setNotification(notification);
            notificationUserAction.setEmail(sessionUser.getEmail());
            notificationUserActionRepository.save(notificationUserAction);
        } else {
            notificationUserAction = notificationUserActionOptional.get();
        }
        return notificationUserAction;
    }


}
