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

    /// /////// Project Manager Admins:

    @Query("SELECT p FROM Project p WHERE p.state != 'DRAFT' ORDER BY p.modifiedAt DESC")
    Page<Project> findAllByOrderByModifiedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.state != 'DRAFT' ORDER BY p.modifiedAt ASC")
    Page<Project> findAllByOrderByModifiedAtAsc(Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.state != 'DRAFT' AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findAllArchivedProjectsModifiedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.state != 'DRAFT' AND p.archivedAt IS NOT NULL ORDER BY p.modifiedAt ASC")
    Page<Project> findAllArchivedProjectsModifiedAtAsc(Pageable pageable);


    @Query("SELECT DISTINCT p FROM Project p WHERE p.state != 'DRAFT'AND p.archivedAt IS NULL ORDER BY p.modifiedAt DESC")
    Page<Project> findAllNotArchivedProjectsModifiedAtDesc(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p WHERE p.state != 'DRAFT' AND p.archivedAt IS NULL ORDER BY p.modifiedAt ASC")
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


    /// /////// Bridgehead Admins:

    @Query("""
                SELECT p
                FROM Project p
                WHERE
                    p.creatorEmail = :email
                    OR EXISTS (
                        SELECT 1
                        FROM ProjectBridgehead pb
                        WHERE pb.project = p
                          AND pb.bridgehead IN :bridgeheads
                    )
                ORDER BY p.modifiedAt DESC
            """)
    List<Project> findByBridgeheadsOrCreator(String email, Set<String> bridgeheads);


    @Query("""
                SELECT p
                FROM Project p
                WHERE
                    p.creatorEmail = :email
                    OR EXISTS (
                        SELECT 1
                        FROM ProjectBridgehead pb
                        WHERE pb.project = p
                          AND pb.bridgehead IN :bridgeheads
                    )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findByBridgeheadsOrCreatorModifiedAtDesc(String email, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE
                    p.creatorEmail = :email
                    OR EXISTS (
                        SELECT 1
                        FROM ProjectBridgehead pb
                        WHERE pb.project = p
                          AND pb.bridgehead IN :bridgeheads
                    )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findByBridgeheadsOrCreatorModifiedAtAsc(String email, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                      )
                  )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findByStateAndBridgeheadsOrCreatorModifiedAtDesc(String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                      )
                  )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findByStateAndBridgeheadsOrCreatorModifiedAtAsc(String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND p.archivedAt IS NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                      )
                  )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findNotArchivedProjectsByStateAndBridgeheadsOrCreatorModifiedAtDesc(String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND p.archivedAt IS NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                      )
                  )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findNotArchivedProjectsByStateAndBridgeheadsOrCreatorModifiedAtAsc(String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND p.archivedAt IS NOT NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                      )
                  )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findArchivedProjectsByStateAndBridgeheadsOrCreatorModifiedAtDesc(String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND p.archivedAt IS NOT NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                      )
                  )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findArchivedProjectsByStateAndBridgeheadsOrCreatorModifiedAtAsc(String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.archivedAt IS NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                      )
                  )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findNotArchivedProjectsByBridgeheadsOrCreatorModifiedAtDesc(String email, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.archivedAt IS NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                      )
                  )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findNotArchivedProjectsByBridgeheadsOrCreatorModifiedAtAsc(String email, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.archivedAt IS NOT NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                      )
                  )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findArchivedProjectsByBridgeheadsOrCreatorModifiedAtDesc(String email, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.archivedAt IS NOT NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                      )
                  )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findArchivedProjectsByBridgeheadsOrCreatorModifiedAtAsc(String email, Set<String> bridgeheads, Pageable pageable);


    /// /////// Researchers:
    @Query("""
                SELECT p
                FROM Project p
                WHERE
                    p.creatorEmail = :email
                    OR EXISTS (
                        SELECT 1
                        FROM ProjectBridgehead pb
                        JOIN ProjectBridgeheadUser pbu
                            ON pbu.projectBridgehead = pb
                        WHERE pb.project = p
                          AND pb.bridgehead IN :bridgeheads
                          AND pbu.email = :email
                    )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findByEmailAndBridgeheadsOrCreatorModifiedAtDesc(String email, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE
                    p.creatorEmail = :email
                    OR EXISTS (
                        SELECT 1
                        FROM ProjectBridgehead pb
                        JOIN ProjectBridgeheadUser pbu
                            ON pbu.projectBridgehead = pb
                        WHERE pb.project = p
                          AND pb.bridgehead IN :bridgeheads
                          AND pbu.email = :email
                    )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findByEmailAndBridgeheadsOrCreatorModifiedAtAsc(String email, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          JOIN ProjectBridgeheadUser pbu
                              ON pbu.projectBridgehead = pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                            AND pbu.email = :email
                      )
                  )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findByEmailAndStateAndBridgeheadsOrCreatorModifiedAtDesc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          JOIN ProjectBridgeheadUser pbu
                              ON pbu.projectBridgehead = pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                            AND pbu.email = :email
                      )
                  )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findByEmailAndStateAndBridgeheadsOrCreatorModifiedAtAsc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND p.archivedAt IS NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          JOIN ProjectBridgeheadUser pbu
                              ON pbu.projectBridgehead = pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                            AND pbu.email = :email
                      )
                  )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findNotArchivedProjectsByEmailAndStateAndBridgeheadsOrCreatorModifiedAtDesc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND p.archivedAt IS NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          JOIN ProjectBridgeheadUser pbu
                              ON pbu.projectBridgehead = pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                            AND pbu.email = :email
                      )
                  )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findNotArchivedProjectsByEmailAndStateAndBridgeheadsOrCreatorModifiedAtAsc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND p.archivedAt IS NOT NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          JOIN ProjectBridgeheadUser pbu
                              ON pbu.projectBridgehead = pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                            AND pbu.email = :email
                      )
                  )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findArchivedProjectsByEmailAndStateAndBridgeheadsOrCreatorModifiedAtDesc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.state = :state
                  AND p.archivedAt IS NOT NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          JOIN ProjectBridgeheadUser pbu
                              ON pbu.projectBridgehead = pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                            AND pbu.email = :email
                      )
                  )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findArchivedProjectsByEmailAndStateAndBridgeheadsOrCreatorModifiedAtAsc(
            String email, ProjectState state, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.archivedAt IS NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          JOIN ProjectBridgeheadUser pbu
                              ON pbu.projectBridgehead = pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                            AND pbu.email = :email
                      )
                  )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findNotArchivedProjectsByEmailAndBridgeheadsOrCreatorModifiedAtDesc(
            String email, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.archivedAt IS NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          JOIN ProjectBridgeheadUser pbu
                              ON pbu.projectBridgehead = pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                            AND pbu.email = :email
                      )
                  )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findNotArchivedProjectsByEmailAndBridgeheadsOrCreatorModifiedAtAsc(
            String email, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.archivedAt IS NOT NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          JOIN ProjectBridgeheadUser pbu
                              ON pbu.projectBridgehead = pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                            AND pbu.email = :email
                      )
                  )
                ORDER BY p.modifiedAt DESC
            """)
    Page<Project> findArchivedProjectsByEmailAndBridgeheadsOrCreatorModifiedAtDesc(
            String email, Set<String> bridgeheads, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.archivedAt IS NOT NULL
                  AND (
                      p.creatorEmail = :email
                      OR EXISTS (
                          SELECT 1
                          FROM ProjectBridgehead pb
                          JOIN ProjectBridgeheadUser pbu
                              ON pbu.projectBridgehead = pb
                          WHERE pb.project = p
                            AND pb.bridgehead IN :bridgeheads
                            AND pbu.email = :email
                      )
                  )
                ORDER BY p.modifiedAt ASC
            """)
    Page<Project> findArchivedProjectsByEmailAndBridgeheadsOrCreatorModifiedAtAsc(
            String email, Set<String> bridgeheads, Pageable pageable);

}
