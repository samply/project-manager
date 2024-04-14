package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProjectBridgeheadRepository extends JpaRepository<ProjectBridgehead, Long> {

    Optional<ProjectBridgehead> findFirstByBridgeheadAndProject(String bridgehead, Project project);

    Set<ProjectBridgehead> findByProject(Project project);

    Set<ProjectBridgehead> findByProjectCodeAndState(String projectCode, ProjectBridgeheadState state);

    Set<ProjectBridgehead> findByProjectAndState(Project project, ProjectBridgeheadState state);

    Page<ProjectBridgehead> findAll(Pageable pageable);

    @Query("SELECT DISTINCT pb FROM ProjectBridgehead pb WHERE pb.project.type = :projectType AND pb.project.state NOT IN :projectStates")
    List<ProjectBridgehead> getByProjectTypeAndNotProjectState(ProjectType projectType, Set<ProjectState> projectStates);

}
