package de.samply.db.repository;

import de.samply.db.model.ProjectManagerAdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectManagerAdminUserRepository extends JpaRepository<ProjectManagerAdminUser, Long> {

    Optional<ProjectManagerAdminUser> findFirstByEmail(String email);

}
