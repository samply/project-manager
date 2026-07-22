package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.project.state.ProjectState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    Optional<Project> findByCode(String projectCode);

    @Query("SELECT p FROM Project p WHERE p.expiresAt < :expirationTime AND p.state IN :states")
    List<Project> findByExpiresAtBeforeAndStateIn(LocalDate expirationTime, Set<ProjectState> states);

    @Query("""
            SELECT p
            FROM Project p
            WHERE p.creatorEmail = :email
               OR EXISTS (
                   SELECT 1
                   FROM ProjectBridgehead pb
                   WHERE pb.project = p
                     AND pb.bridgehead IN :bridgeheads
               )
            ORDER BY p.modifiedAt DESC
            """)
    List<Project> findByBridgeheadsOrCreator(String email, Set<String> bridgeheads);
}
