package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryState;
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

    Set<ProjectBridgehead> findByProjectAndState(Project project, ProjectBridgeheadState state);

    @Query("""
                SELECT DISTINCT pb
                FROM ProjectBridgehead pb
                JOIN pb.project p
                JOIN p.query q
                JOIN q.outputs qo
                WHERE qo.projectType = :projectType
                  AND p.state NOT IN :projectStates
            """)
    List<ProjectBridgehead> getByProjectTypeAndNotProjectState(
            ProjectType projectType,
            Set<ProjectState> projectStates
    );


    @Query("""
                SELECT DISTINCT pb
                FROM ProjectBridgehead pb
                JOIN pb.executions exec
                WHERE exec.queryState = :queryState
                  AND pb.project.state IN :projectStates
            """)
    Set<ProjectBridgehead> getByQueryStateAndProjectState(
            QueryState queryState,
            Set<ProjectState> projectStates
    );

}
