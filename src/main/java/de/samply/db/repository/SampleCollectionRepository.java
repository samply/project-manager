package de.samply.db.repository;

import de.samply.db.model.SampleCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SampleCollectionRepository extends JpaRepository<SampleCollection, Long> {
    
}
