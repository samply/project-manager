package de.samply.notification;

import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Notification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class DtoNotificationService {

    private final NotificationService notificationService;
    private final DtoFactory dtoFactory;

    public DtoNotificationService(NotificationService notificationService,
                                  DtoFactory dtoFactory) {
        this.notificationService = notificationService;
        this.dtoFactory = dtoFactory;
    }

    public List<Notification> fetchUserVisibleNotifications(
            Optional<de.samply.db.model.Project> project,
            Optional<de.samply.db.model.ProjectBridgehead> bridgehead,
            Supplier<List<de.samply.db.model.Project>> allUserVisibleProjectFetcher) throws NotificationServiceException {
        return notificationService
                .fetchUserVisibleNotifications(project, bridgehead, allUserVisibleProjectFetcher)
                .stream()
                .map(notification -> dtoFactory
                        .convert(notification, () ->
                                notificationService.fetchNotificationUserAction(notification)))
                .toList();
    }

}
