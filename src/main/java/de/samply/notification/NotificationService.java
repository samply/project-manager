package de.samply.notification;

import de.samply.db.model.Notification;
import de.samply.db.model.NotificationUserAction;
import de.samply.db.model.Project;
import de.samply.db.repository.NotificationRepository;
import de.samply.db.repository.NotificationUserActionRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.frontend.dto.DtoFactory;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
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
    private final ProjectRepository projectRepository;
    private final SessionUser sessionUser;
    private final DtoFactory dtoFactory;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationUserActionRepository notificationUserActionRepository,
                               ProjectRepository projectRepository,
                               SessionUser sessionUser,
                               DtoFactory dtoFactory) {
        this.notificationRepository = notificationRepository;
        this.notificationUserActionRepository = notificationUserActionRepository;
        this.projectRepository = projectRepository;
        this.sessionUser = sessionUser;
        this.dtoFactory = dtoFactory;
    }

    public void createNotification(@NotNull String projectCode, String bridgehead, String email,
                                   @NotNull OperationType operationType,
                                   @NotNull String details, String error, HttpStatus httpStatus
    ) throws NotificationServiceException {
        Project project = fetchProject(projectCode);
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

    private Project fetchProject(String projectCode) throws NotificationServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new NotificationServiceException("Project " + projectCode + " not found");
        }
        return project.get();
    }

    // We use a supplier of ProjectService.fetchAllUserVisibleProjects in order to remove interdependence
    // between the notification service and the project service.
    public List<de.samply.frontend.dto.Notification> fetchUserVisibleNotifications(
            Optional<String> projectCodeOptional, Optional<String> bridgheadOptional,
            Supplier<List<Project>> allUserVisibleProjectFetcher) throws NotificationServiceException {
        List<Notification> result = new ArrayList<>();
        List<Project> projects = (projectCodeOptional.isEmpty()) ?
                allUserVisibleProjectFetcher.get() : List.of(fetchProject(projectCodeOptional.get()));
        List<String> bridgeheads = fetchUserVisibleBridgeheads(bridgheadOptional);
        projects.forEach(project -> {
            if (bridgeheads.isEmpty() && sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.PROJECT_MANAGER_ADMIN)) {
                result.addAll(notificationRepository.findByProjectOrderByTimestampDesc(project));
            } else {
                bridgeheads.forEach(bridgehead -> result.addAll(
                        notificationRepository.findByProjectAndBridgeheadOrBridgeheadIsNullOrderByTimestampDesc(project, bridgehead)));
            }
        });
        return result.stream().map(notification ->
                dtoFactory.convert(notification, () -> fetchNotificationUserAction(notification))).toList();
    }

    private List<String> fetchUserVisibleBridgeheads(Optional<String> requestedBridgehead) {
        if (sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.PROJECT_MANAGER_ADMIN)) {
            return (requestedBridgehead.isEmpty()) ? new ArrayList<>() : List.of(requestedBridgehead.get());
        } else {
            if (requestedBridgehead.isEmpty()) {
                return sessionUser.getBridgeheads().stream().toList();
            } else {
                return (sessionUser.getBridgeheads().contains(requestedBridgehead.get())) ?
                        List.of(requestedBridgehead.get()) : new ArrayList<>();
            }

        }
    }

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
