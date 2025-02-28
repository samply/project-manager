package de.samply.db.repository;

import de.samply.db.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {

    Optional<Query> findByCode(String queryCode);

}
