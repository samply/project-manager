package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    Optional<Project> findByCode(String projectCode);

    boolean existsByCode(String projectCode);

    @Query(value = "SELECT nextval('samply.project_code_seq')", nativeQuery = true)
    long nextProjectCodeSequenceValue();

    @Query("SELECT p FROM Project p WHERE p.expiresAt < :expirationTime AND p.state IN :states")
    List<Project> findByExpiresAtBeforeAndStateIn(LocalDate expirationTime, Set<ProjectState> states);

    @Query("SELECT DISTINCT p FROM Project p JOIN p.query q JOIN q.outputs o WHERE p.state IN :states AND o.projectType IN :projectTypes")
    List<Project> findByStateInAndProjectTypeIn(Set<ProjectState> states, Collection<ProjectType> projectTypes);

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
