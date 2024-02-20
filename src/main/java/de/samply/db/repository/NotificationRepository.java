package de.samply.db.repository;

import de.samply.db.model.Notification;
import de.samply.db.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByProjectOrderByTimestampDesc(Project project);

    @Query("SELECT n FROM Notification n WHERE n.project = :project AND (n.bridgehead = :bridgehead OR n.bridgehead IS NULL) " +
            "ORDER BY n.timestamp DESC")
    List<Notification> findByProjectAndBridgeheadOrBridgeheadIsNullOrderByTimestampDesc(Project project, String bridgehead);
}
