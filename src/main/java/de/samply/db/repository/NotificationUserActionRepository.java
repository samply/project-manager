package de.samply.db.repository;

import de.samply.db.model.Notification;
import de.samply.db.model.NotificationUserAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface NotificationUserActionRepository extends JpaRepository<NotificationUserAction, Long> {

    // The read state is per user (email). Older data can hold several rows for the same notification and user,
    // so none of these queries expects a unique row.
    Optional<NotificationUserAction> findFirstByNotificationAndEmailOrderByIdAsc(Notification notification, String email);

    @Query("SELECT DISTINCT action.notification.id FROM NotificationUserAction action "
            + "WHERE action.email = :email AND action.read = true")
    Set<Long> findReadNotificationIds(@Param("email") String email);

}
