package de.samply.db.repository;

import de.samply.db.model.CreatorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface CreatorUserRepository extends JpaRepository<CreatorUser, Long> {

    Set<CreatorUser> findByEmail(String email);

    Optional<CreatorUser> findByEmailAndBridgehead(String email, String bridgehead);

}
