package de.samply.db.repository;

import de.samply.db.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectQueryRepository extends JpaRepository<Query, Long> {

}
