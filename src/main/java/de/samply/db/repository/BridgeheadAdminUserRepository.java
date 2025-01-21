package de.samply.db.repository;

import de.samply.db.model.BridgeheadAdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface BridgeheadAdminUserRepository extends JpaRepository<BridgeheadAdminUser, Long> {

    Optional<BridgeheadAdminUser> findFirstByEmailAndBridgehead(String email, String bridgehead);
    Set<BridgeheadAdminUser> findByBridgehead(String bridgehead);
    Optional<BridgeheadAdminUser> findFirstByBridgehead(String bridgehead);
}
