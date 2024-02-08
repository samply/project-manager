package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.project.state.ProjectBridgeheadState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ProjectBridgeheadRepository extends JpaRepository<ProjectBridgehead, Long> {

    Optional<ProjectBridgehead> findFirstByBridgeheadAndProject(String bridgehead, Project project);

    Set<ProjectBridgehead> findByProject(Project project);

    Set<ProjectBridgehead> findByProjectAndState(Project project, ProjectBridgeheadState state);

    Page<ProjectBridgehead> findAll(Pageable pageable);
    
}
