package de.samply.db.repository;

import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.ProjectCoder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectCoderRepository extends JpaRepository<ProjectCoder, Long> {

    Optional<ProjectCoder> findByProjectBridgeheadUser(ProjectBridgeheadUser projectBridgeheadUser);

    Optional<ProjectCoder> findByProjectBridgeheadUserAndDeletedAtIsNull(ProjectBridgeheadUser projectBridgeheadUser);

}
