package de.samply.db.repository;

import de.samply.db.model.BridgeheadAdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Repository
public interface BridgeheadAdminUserRepository extends JpaRepository<BridgeheadAdminUser, Long> {

    Optional<BridgeheadAdminUser> findFirstByEmailAndBridgehead(String email, String bridgehead);
    Set<BridgeheadAdminUser> findByBridgehead(String bridgehead);
    Optional<BridgeheadAdminUser> findFirstByBridgeheadOrderByIdAsc(String bridgehead);
    void deleteByEmailAndBridgehead(String email, String bridgehead);

    // Parallel requests may insert the same address at once: the unique index (V006) keeps one, the others do nothing
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO samply.bridgehead_admin_user (email, bridgehead) VALUES (:email, :bridgehead) "
            + "ON CONFLICT DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@Param("email") String email, @Param("bridgehead") String bridgehead);
}
