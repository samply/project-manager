package de.samply.db.repository;

import de.samply.db.model.ProjectManagerAdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ProjectManagerAdminUserRepository extends JpaRepository<ProjectManagerAdminUser, Long> {

    Optional<ProjectManagerAdminUser> findFirstByEmail(String email);

    // Parallel requests may insert the same address at once: the unique index (V006) keeps one, the others do nothing
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO samply.project_manager_admin_user (email) VALUES (:email) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    void insertIfAbsent(@Param("email") String email);

    void deleteByEmail(String email);

}
