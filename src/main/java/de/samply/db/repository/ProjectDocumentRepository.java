package de.samply.db.repository;

import de.samply.db.model.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {
}
