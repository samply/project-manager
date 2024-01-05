package de.samply.db.repository;

import de.samply.db.model.Notification;
import de.samply.db.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByProjectOrderByTimestampDesc(Project project);
    List<Notification> findAllByProjectAndBridgeheadOrBridgeheadIsNullOrderByTimestampDesc(Project project, String bridgehead);
}
