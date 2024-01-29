package de.samply.db.repository;

import de.samply.db.model.NotificationUserAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationUserActionRepository extends JpaRepository<NotificationUserAction, Long> {

}
