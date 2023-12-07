package de.samply.db.repository;

import de.samply.db.model.ProjectBridgehead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectBridgeheadRepository extends JpaRepository<ProjectBridgehead, Long> {

}
