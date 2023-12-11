package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {

    Optional<ProjectDocument> findFirstByProjectAndAndOriginalFilename(Project project, String filename);

}
