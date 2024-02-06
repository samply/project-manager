package de.samply.db.repository;

import de.samply.db.model.Notification;
import de.samply.db.model.NotificationUserAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationUserActionRepository extends JpaRepository<NotificationUserAction, Long> {

    Optional<NotificationUserAction> findByNotification(Notification notification);

}
