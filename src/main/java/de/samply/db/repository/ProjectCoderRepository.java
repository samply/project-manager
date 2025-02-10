package de.samply.db.repository;

import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.ProjectCoder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectCoderRepository extends JpaRepository<ProjectCoder, Long> {

    Optional<ProjectCoder> findFirstByProjectBridgeheadUserAndDeletedAtIsNullOrderByCreatedAtDesc(ProjectBridgeheadUser projectBridgeheadUser);

    @Query("""
        SELECT pc
        FROM ProjectCoder pc
        WHERE pc.projectBridgeheadUser.projectBridgehead.bridgehead = :bridgehead
          AND pc.projectBridgeheadUser.projectBridgehead.project.code = :projectCode
          AND pc.projectBridgeheadUser.email = :email
        ORDER BY pc.createdAt DESC
    """)
    List<ProjectCoder> findByBridgeheadAndProjectCodeAndEmailOrderedByCreatedAtDesc(
            @Param("bridgehead") String bridgehead,
            @Param("projectCode") String projectCode,
            @Param("email") String email
    );

    List<ProjectCoder> findByProjectBridgeheadUserOrderByCreatedAtDesc(ProjectBridgeheadUser projectBridgeheadUser);

    @Query("SELECT DISTINCT pc FROM ProjectCoder pc " +
            "JOIN pc.projectBridgeheadUser pbu " +
            "JOIN pbu.projectBridgehead pb " +
            "JOIN pb.project p " +
            "WHERE pc.deletedAt IS NULL AND p.code = :projectCode AND pb.bridgehead = :bridgehead")
    List<ProjectCoder> findDistinctByProjectCodeAndBridgeheadIfNotDeleted(@Param("projectCode") String projectCode, @Param("bridgehead") String bridgehead);

}
