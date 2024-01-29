package de.samply.notification;

import de.samply.db.model.Notification;
import de.samply.db.model.Project;
import de.samply.db.repository.NotificationRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.frontend.dto.DtoFactory;
import de.samply.project.ProjectService;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final SessionUser sessionUser;

    public NotificationService(NotificationRepository notificationRepository,
                               ProjectRepository projectRepository,
                               ProjectService projectService,
                               SessionUser sessionUser) {
        this.notificationRepository = notificationRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.sessionUser = sessionUser;
    }

    public void createNotification(@NotNull String projectCode, String bridgehead, @NotNull String email,
                                   @NotNull OperationType operationType,
                                   @NotNull String details, String error
    ) throws NotificationServiceException {
        Project project = fetchProject(projectCode);
        Notification notification = new Notification();
        notification.setProject(project);
        notification.setBridgehead(bridgehead);
        notification.setEmail(email);
        notification.setOperationType(operationType);
        notification.setDetails(details);
        notification.setError(error);
        notificationRepository.save(notification);
    }

    private Project fetchProject(String projectCode) throws NotificationServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new NotificationServiceException("Project " + projectCode + " not found");
        }
        return project.get();
    }

    public List<de.samply.frontend.dto.Notification> fetchUserVisibleNotifications(Optional<String> projectCodeOptional, Optional<String> bridgheadOptional) throws NotificationServiceException {
        List<Notification> result = new ArrayList<>();
        List<Project> projects = (projectCodeOptional.isEmpty()) ?
                projectService.fetchAllUserVisibleProjects() : List.of(fetchProject(projectCodeOptional.get()));
        List<String> bridgeheads = fetchUserVisibleBridgeheads(bridgheadOptional);
        projects.forEach(project -> {
            if (bridgeheads.isEmpty() && sessionUser.getUserOrganisationRoles().containsRole(OrganisationRole.PROJECT_MANAGER_ADMIN)) {
                notificationRepository.findAllByProjectOrderByTimestampDesc(project);
            } else {
                bridgeheads.forEach(bridgehead -> result.addAll(
                        notificationRepository.findAllByProjectAndBridgeheadOrBridgeheadIsNullOrderByTimestampDesc(project, bridgehead)));
            }
        });
        return result.stream().map(DtoFactory::convert).toList();
    }

    List<String> fetchUserVisibleBridgeheads(Optional<String> requestedBridgehead) {
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


}
