package de.samply.db.repository;

import de.samply.db.model.CentralApprovalCommitteeUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CentralApprovalCommitteeUserRepository extends JpaRepository<CentralApprovalCommitteeUser, Long> {

    Optional<CentralApprovalCommitteeUser> findFirstByEmail(String email);

    void deleteByEmail(String email);

}
