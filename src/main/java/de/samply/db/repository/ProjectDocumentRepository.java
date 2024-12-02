package de.samply.db.repository;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectDocument;
import de.samply.document.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {

    Optional<ProjectDocument> findFirstByProjectAndBridgeheadAndOriginalFilename(Project project, String bridgehead, String filename);

    Optional<ProjectDocument> findFirstByProjectAndOriginalFilename(Project project, String filename);

    Optional<ProjectDocument> findFirstByProjectAndDocumentTypeAndBridgeheadOrderByCreatedAtDesc(Project project, DocumentType documentType, String bridgehead);

    Optional<ProjectDocument> findTopByProjectOrderByCreatedAtDesc(Project project);

    List<ProjectDocument> findAllByProjectAndDocumentTypeOrderByLabelAsc(Project project, DocumentType documentType);

    @Query("SELECT pd FROM ProjectDocument pd WHERE (pd.bridgehead = :bridgehead OR pd.bridgehead = 'NONE') " +
            "AND pd.project = :project AND pd.documentType = :documentType ORDER BY pd.label ASC")
    List<ProjectDocument> findAllByBridgeheadAndProjectAndDocumentTypeOrderByLabelAsc(String bridgehead, Project project, DocumentType documentType);

}
