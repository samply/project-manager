package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryState;
import de.samply.user.roles.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProjectBridgeheadUserRepository extends JpaRepository<ProjectBridgeheadUser, Long> {

    Optional<ProjectBridgeheadUser> findFirstByEmailAndProjectBridgeheadAndProjectRole(String email, ProjectBridgehead projectBridgehead, ProjectRole projectRole);

    List<ProjectBridgeheadUser> getByEmailAndProjectBridgehead(String email, ProjectBridgehead projectBridgehead);

    Optional<ProjectBridgeheadUser> getFirstByEmailAndProjectBridgeheadOrderByModifiedAtDesc(String email, ProjectBridgehead projectBridgehead);

    Optional<ProjectBridgeheadUser> getFirstByEmailAndProjectBridgehead_ProjectAndProjectBridgehead_BridgeheadOrderByModifiedAtDesc(String email, Project project, String bridgehead);

    List<ProjectBridgeheadUser> getByProjectBridgehead(ProjectBridgehead projectBridgehead);

    @Query("SELECT DISTINCT pbu.projectBridgehead.project FROM ProjectBridgeheadUser pbu WHERE pbu.email = :email")
    List<Project> findProjectsByEmail(String email);

    @Query("""
                SELECT DISTINCT pbu
                FROM ProjectBridgeheadUser pbu
                JOIN pbu.projectBridgehead pb
                JOIN pb.project p
                JOIN p.query q
                JOIN q.outputs qo
                WHERE qo.projectType = :projectType
                  AND p.state = :projectState
                  AND pbu.projectRole = :projectRole
            """)
    Set<ProjectBridgeheadUser> getByProjectTypeAndProjectStateAndProjectRole(
            ProjectType projectType,
            ProjectState projectState,
            ProjectRole projectRole
    );

    @Query("""
                SELECT DISTINCT pbu
                FROM ProjectBridgeheadUser pbu
                JOIN pbu.projectBridgehead pb
                JOIN pb.project p
                JOIN p.query q
                JOIN q.outputs qo
                WHERE qo.projectType = :projectType
                  AND p.state = :projectState
                  AND pbu.projectRole <> :projectRole
            """)
    Set<ProjectBridgeheadUser> getByProjectTypeAndProjectStateAndNotProjectRole(
            ProjectType projectType,
            ProjectState projectState,
            ProjectRole projectRole
    );

    @Query("SELECT DISTINCT pbu FROM ProjectBridgeheadUser pbu WHERE pbu.email LIKE %:email% AND pbu.projectBridgehead.bridgehead = :bridgehead AND (" +
            "(pbu.projectBridgehead.project.code = :projectCode AND pbu.projectBridgehead.project.state = 'DEVELOP' AND pbu.projectRole = 'DEVELOPER') OR " +
            "(pbu.projectBridgehead.project.code = :projectCode AND pbu.projectBridgehead.project.state = 'PILOT' AND pbu.projectRole = 'PILOT') OR" +
            "(pbu.projectBridgehead.project.code = :projectCode AND pbu.projectBridgehead.project.state = 'FINAL' AND pbu.projectRole = 'FINAL'))")
    List<ProjectBridgeheadUser> getDistinctByEmailContainingAndProjectBridgehead_BridgeheadAndUserAlreadySetForThisProjectInThisRole(String email, String bridgehead, String projectCode);

    List<ProjectBridgeheadUser> getDistinctByEmailContainingAndProjectBridgehead(String email, ProjectBridgehead projectBridgehead);

    @Query("SELECT pbu FROM ProjectBridgeheadUser pbu WHERE pbu.projectBridgehead = :projectBridgehead AND pbu.email = :email AND (" +
            "(pbu.projectBridgehead.project.state = 'DEVELOP' AND pbu.projectRole = 'DEVELOPER') OR " +
            "(pbu.projectBridgehead.project.state = 'PILOT' AND pbu.projectRole = 'PILOT') OR " +
            "(pbu.projectBridgehead.project.state = 'FINAL' AND pbu.projectRole = 'FINAL')) " +
            "ORDER BY pbu.id ASC")
    Optional<ProjectBridgeheadUser> getFirstValidByEmailAndProjectBridgehead(String email, ProjectBridgehead projectBridgehead);

    @Query("SELECT DISTINCT pbu FROM ProjectBridgeheadUser pbu WHERE pbu.projectBridgehead = :projectBridgehead AND pbu.projectRole = :role AND (" +
            "(pbu.projectBridgehead.project.state = 'DEVELOP' AND pbu.projectRole = 'DEVELOPER') OR " +
            "(pbu.projectBridgehead.project.state = 'PILOT' AND pbu.projectRole = 'PILOT') OR " +
            "(pbu.projectBridgehead.project.state = 'FINAL' AND pbu.projectRole = 'FINAL'))")
    List<ProjectBridgeheadUser> getDistinctByProjectRoleAndProjectBridgehead(ProjectRole role, ProjectBridgehead projectBridgehead);

    @Query("SELECT DISTINCT pbu FROM ProjectBridgeheadUser pbu WHERE pbu.projectBridgehead.project.code = :projectCode AND pbu.projectRole = :projectRole")
    Set<ProjectBridgeheadUser> getDistinctByProjectRoleAndProjectCode(ProjectRole projectRole, String projectCode);

    @Query("""
                SELECT DISTINCT pbu
                FROM ProjectBridgeheadUser pbu
                JOIN pbu.projectBridgehead pb
                JOIN pb.project p
                JOIN pb.executions exec
                JOIN exec.queryOutput qo
                WHERE qo.projectType = :projectType
                  AND pb.state = :projectBridgeheadState
                  AND exec.queryState = :queryState
                  AND (
                        (p.state = 'DEVELOP' AND pbu.projectRole = 'DEVELOPER') OR
                        (p.state = 'PILOT'   AND pbu.projectRole = 'PILOT') OR
                        (p.state = 'FINAL'   AND pbu.projectRole = 'FINAL')
                      )
            """)
    List<ProjectBridgeheadUser>
    getDistinctInValidProjectStateByProjectTypeAndQueryStateAndProjectBridgeheadState(
            ProjectType projectType,
            QueryState queryState,
            ProjectBridgeheadState projectBridgeheadState
    );

    @Query("""
                SELECT DISTINCT pbu
                FROM ProjectBridgeheadUser pbu
                JOIN pbu.projectBridgehead pb
                JOIN pb.project p
                JOIN p.query q
                JOIN q.outputs qo
                WHERE qo.projectType = :projectType
                  AND (
                        (pbu.projectRole = 'DEVELOPER' AND p.state <> 'DEVELOP') OR
                        (pbu.projectRole = 'PILOT'     AND p.state <> 'PILOT') OR
                        (pbu.projectRole = 'FINAL'     AND p.state <> 'FINAL')
                      )
            """)
    List<ProjectBridgeheadUser>
    getDistinctInInvalidProjectStateByProjectType(ProjectType projectType);

}
