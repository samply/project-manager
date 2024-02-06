package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectState;
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

    @Query("SELECT DISTINCT pbu FROM ProjectBridgeheadUser pbu WHERE pbu.projectBridgehead.project.type = :projectType AND pbu.projectBridgehead.project.state = :projectState AND pbu.projectRole = :projectRole")
    List<ProjectBridgeheadUser> getByProjectTypeAndProjectStateAndProjectRole(ProjectType projectType, ProjectState projectState, ProjectRole projectRole);

    @Query("SELECT DISTINCT pbu FROM ProjectBridgeheadUser pbu WHERE pbu.projectBridgehead.project.type = :projectType AND pbu.projectBridgehead.project.state = :projectState AND pbu.projectRole != :projectRole")
    List<ProjectBridgeheadUser> getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType projectType, ProjectState projectState, ProjectRole projectRole);

    @Query("SELECT DISTINCT pbu FROM ProjectBridgeheadUser pbu WHERE pbu.projectBridgehead.project.type = :projectType AND pbu.projectBridgehead.project.state IN :projectStates")
    List<ProjectBridgeheadUser> getByProjectTypeAndNotProjectState(ProjectType projectType, Set<ProjectState> projectStates);

}
