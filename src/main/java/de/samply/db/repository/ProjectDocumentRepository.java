package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {

    Optional<ProjectDocument> findFirstByProjectAndBridgeheadAndOriginalFilename(Project project, String bridgehead, String filename);
    Optional<ProjectDocument> findFirstByProjectAndOriginalFilename(Project project, String filename);

}
