package de.samply.db.repository;

import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadDataShield;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectBridgeheadDataShieldRepository extends JpaRepository<ProjectBridgeheadDataShield, Long> {


    Optional<ProjectBridgeheadDataShield> findByProjectBridgehead(ProjectBridgehead projectBridgehead);

}
