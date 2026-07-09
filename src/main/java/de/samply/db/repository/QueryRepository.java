package de.samply.db.repository;

import de.samply.db.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {

    Optional<Query> findByCode(String queryCode);

    @Modifying
    @org.springframework.data.jpa.repository.Query("""
            DELETE FROM Query q
            WHERE q.id = :queryId
              AND NOT EXISTS (
                  SELECT 1 FROM Project p WHERE p.query = q
              )
            """)
    void deleteIfOrphan(@Param("queryId") Long queryId);

}
