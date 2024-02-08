package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.project.state.ProjectState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByCode(String projectCode);


    @Query("SELECT p FROM Project p WHERE p.expiresAt < :expirationTime AND p.state IN :states")
    List<Project> findByExpiresAtBeforeAndStateIn(LocalDate expirationTime, Set<ProjectState> states);

    ////////// Project Manager Admins:
    List<Project> findAll();

    Page<Project> findAllByOrderByModifiedAtDesc(Pageable pageable);

    Page<Project> findAllByOrderByModifiedAtAsc(Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.archivedAt IS NOT NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findAllArchivedProjectsModifiedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.archivedAt IS NOT NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findAllArchivedProjectsModifiedAtAsc(Pageable pageable);


    @Query("SELECT DISTINCT p FROM Project p WHERE p.archivedAt IS NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findAllNotArchivedProjectsModifiedAtDesc(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p WHERE p.archivedAt IS NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findAllNotArchivedProjectsModifiedAtAsc(Pageable pageable);

    Page<Project> findByStateOrderByModifiedAtDesc(ProjectState state, Pageable pageable);

    Page<Project> findByStateOrderByModifiedAtAsc(ProjectState state, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p WHERE p.state = :state AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findArchivedProjectsByStateModifiedAtDesc(ProjectState state, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p WHERE p.state = :state AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findArchivedProjectsByStateModifiedAtAsc(ProjectState state, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p WHERE p.state = :state AND p.archivedAt IS NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findNotArchivedProjectsByStateModifiedAtDesc(ProjectState state, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p WHERE p.state = :state AND p.archivedAt IS NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findNotArchivedProjectsByStateModifiedAtAsc(ProjectState state, Pageable pageable);


    ////////// Bridgehead Admins:

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb  ON pb.project = p " +
            "WHERE pb.bridgehead IN :bridgeheads ORDER BY p.modifiedAt DESC")
    List<Project> findByBridgeheads(Set<String> bridgeheads);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb  ON pb.project = p " +
            "WHERE pb.bridgehead IN :bridgeheads ORDER BY p.modifiedAt DESC")
    Page<Project> findByBridgeheadsModifiedAtDesc(Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb  ON pb.project = p " +
            "WHERE pb.bridgehead IN :bridgeheads ORDER BY p.modifiedAt ASC")
    Page<Project> findByBridgeheadsModifiedAtAsc(Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads ORDER BY p.modifiedAt DESC")
    Page<Project> findByStateAndBridgeheadsModifiedAtDesc(ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads ORDER BY p.modifiedAt ASC")
    Page<Project> findByStateAndBridgeheadsModifiedAtAsc(ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findNotArchivedProjectsByStateAndBridgeheadsModifiedAtDesc(ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findNotArchivedProjectsByStateAndBridgeheadsModifiedAtAsc(ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findArchivedProjectsByStateAndBridgeheadsModifiedAtDesc(ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findArchivedProjectsByStateAndBridgeheadsModifiedAtAsc(ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE pb.bridgehead IN :bridgeheads AND p.archivedAt IS NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findNotArchivedProjectsByBridgeheadsModifiedAtDesc(Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE pb.bridgehead IN :bridgeheads AND p.archivedAt IS NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findNotArchivedProjectsByBridgeheadsModifiedAtAsc(Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE pb.bridgehead IN :bridgeheads AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findArchivedProjectsByBridgeheadsModifiedAtDesc(Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "WHERE pb.bridgehead IN :bridgeheads AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findArchivedProjectsByBridgeheadsModifiedAtAsc(Set<String> bridgeheads, Pageable pageable);


    ////////// Researchers:
    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE pb.bridgehead IN :bridgeheads " +
            "AND (pbu.email = :email OR p.creatorEmail = :email) ORDER BY p.modifiedAt DESC")
    Page<Project> findByEmailAndBridgeheadsModifiedAtDesc(String email, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE pb.bridgehead IN :bridgeheads " +
            "AND (pbu.email = :email OR p.creatorEmail = :email) ORDER BY p.modifiedAt ASC")
    Page<Project> findByEmailAndBridgeheadsModifiedAtAsc(String email, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads " +
            "AND (pbu.email = :email OR p.creatorEmail = :email) ORDER BY p.modifiedAt DESC")
    Page<Project> findByEmailAndStateAndBridgeheadsModifiedAtDesc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE p.state = :state AND pb.bridgehead IN :bridgeheads " +
            "AND (pbu.email = :email OR p.creatorEmail = :email) ORDER BY p.modifiedAt ASC")
    Page<Project> findByEmailAndStateAndBridgeheadsModifiedAtAsc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND p.state = :state " +
            "AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findNotArchivedProjectsByEmailAndStateAndBridgeheadsModifiedAtDesc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND p.state = :state " +
            "AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findNotArchivedProjectsByEmailAndStateAndBridgeheadsModifiedAtAsc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND p.state = :state " +
            "AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findArchivedProjectsByEmailAndStateAndBridgeheadsModifiedAtDesc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND p.state = :state " +
            "AND pb.bridgehead IN :bridgeheads AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findArchivedProjectsByEmailAndStateAndBridgeheadsModifiedAtAsc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND pb.bridgehead IN :bridgeheads " +
            "AND p.archivedAt IS NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findNotArchivedProjectsByEmailAndBridgeheadsModifiedAtDesc(
            String email, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND pb.bridgehead IN :bridgeheads " +
            "AND p.archivedAt IS NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findNotArchivedProjectsByEmailAndBridgeheadsModifiedAtAsc(
            String email, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND pb.bridgehead IN :bridgeheads " +
            "AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findArchivedProjectsByEmailAndBridgeheadsModifiedAtDesc(
            String email, Set<String> bridgeheads, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p INNER JOIN ProjectBridgehead pb ON pb.project = p " +
            "LEFT JOIN ProjectBridgeheadUser pbu ON pbu.projectBridgehead = pb " +
            "WHERE (pbu.email = :email OR p.creatorEmail = :email) AND pb.bridgehead IN :bridgeheads " +
            "AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findArchivedProjectsByEmailAndBridgeheadsModifiedAtAsc(
            String email, Set<String> bridgeheads, Pageable pageable);

}
