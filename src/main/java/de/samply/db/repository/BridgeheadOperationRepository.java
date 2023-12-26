package de.samply.db.repository;

import de.samply.db.model.BridgeheadOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BridgeheadOperationRepository extends JpaRepository<BridgeheadOperation, Long> {

}
