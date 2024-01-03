package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.project.state.ProjectState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByCode(String projectCode);


    ////////// Project Manager Admins:
    Page<Project> findAll(Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.archivedAt IS NOT NULL")
    Page<Project> findAllArchivedProjects(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p WHERE p.archivedAt IS NULL")
    Page<Project> findAllNotArchivedProjects(Pageable pageable);

    Page<Project> findByState(ProjectState state, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p WHERE p.state = :state AND p.archivedAt IS NOT NULL")
    Page<Project> findArchivedProjectsByState(ProjectState state, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p WHERE p.state = :state AND p.archivedAt IS NULL")
    Page<Project> findNotArchivedProjectsByState(ProjectState state, Pageable pageable);


    ////////// Bridgehead Admins:
    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb  ON pb.project = p " +
            "WHERE pb.bridgehead IN :bridgeheads")
    Page<Project> findByBridgeheads(Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads")
    Page<Project> findByStateAndBridgeheads(ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NULL")
    Page<Project> findNotArchivedProjectsByStateAndBridgeheads(ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NOT NULL")
    Page<Project> findArchivedProjectsByStateAndBridgeheads(ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE pb.bridgehead IN :bridgeheads AND p.archivedAt IS NULL")
    Page<Project> findNotArchivedProjectsByBridgeheads(Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE pb.bridgehead IN :bridgeheads AND p.archivedAt IS NOT NULL")
    Page<Project> findArchivedProjectsByBridgeheads(Set<String> bridgeheads, Pageable pageable);


    ////////// Researchers:
    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE pb.bridgehead IN :bridgeheads " +
            "AND (pbu.email = :email OR p.creatorEmail = :email)")
    Page<Project> findByEmailAndBridgeheads(String email, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads " +
            "AND (pbu.email = :email OR p.creatorEmail = :email)")
    Page<Project> findByEmailAndStateAndBridgeheads(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND p.state = :state " +
            "AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NULL")
    Page<Project> findNotArchivedProjectsByEmailAndStateAndBridgeheads(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND p.state = :state " +
            "AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NOT NULL")
    Page<Project> findArchivedProjectsByEmailAndStateAndBridgeheads(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND pb.bridgehead IN :bridgeheads " +
            "AND p.archivedAt IS NULL")
    Page<Project> findNotArchivedProjectsByEmailAndBridgeheads(
            String email, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND pb.bridgehead IN :bridgeheads " +
            "AND p.archivedAt IS NOT NULL")
    Page<Project> findArchivedProjectsByEmailAndBridgeheads(
            String email, Set<String> bridgeheads, Pageable pageable);

}
