package de.samply.db.repository;

import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.user.roles.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProjectBridgeheadUserRepository extends JpaRepository<ProjectBridgeheadUser, Long> {

    Optional<ProjectBridgeheadUser> findFirstByEmailAndProjectBridgeheadAndProjectRole(String email, ProjectBridgehead projectBridgehead, ProjectRole projectRole);

    List<ProjectBridgeheadUser> getByEmailAndProjectBridgehead(String email, ProjectBridgehead projectBridgehead);

    @Query("SELECT DISTINCT p.projectRole FROM ProjectBridgeheadUser p " +
            "WHERE p.email = :email AND p.projectBridgehead = :projectBridgehead")
    Set<ProjectRole> findDistinctProjectRolesByEmailAndProjectBridgehead(
            @Param("email") String email,
            @Param("projectBridgehead") ProjectBridgehead projectBridgehead
    );

    Optional<ProjectBridgeheadUser> findByEmailAndProjectBridgeheadAAndProjectRole(String email, ProjectBridgehead projectBridgehead, ProjectRole projectRole);

}
