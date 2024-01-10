package de.samply.notification;

import de.samply.db.model.Notification;
import de.samply.db.model.Project;
import de.samply.db.repository.NotificationRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.frontend.dto.DtoFactory;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProjectRepository projectRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               ProjectRepository projectRepository) {
        this.notificationRepository = notificationRepository;
        this.projectRepository = projectRepository;
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

    public List<de.samply.frontend.dto.Notification> fetchNotifications(String projectCode, Optional<String> bridghead) throws NotificationServiceException {
        Project project = fetchProject(projectCode);
        return ((bridghead.isEmpty()) ?
                notificationRepository.findAllByProjectOrderByTimestampDesc(project) :
                notificationRepository.findAllByProjectAndBridgeheadOrBridgeheadIsNullOrderByTimestampDesc(project, bridghead.get()))
                .stream().map(DtoFactory::convert).toList();
    }


}
