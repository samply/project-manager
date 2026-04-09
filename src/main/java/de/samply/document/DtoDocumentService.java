package de.samply.document;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.ProjectDocument;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DtoDocumentService {

    private final DocumentService documentService;
    private final DtoFactory dtoFactory;

    public DtoDocumentService(DocumentService documentService, DtoFactory dtoFactory) {
        this.documentService = documentService;
        this.dtoFactory = dtoFactory;
    }

    public List<de.samply.frontend.dto.ProjectDocument> fetchPublications(Project project) {
        return convertToDto(documentService.fetchDocuments(project, Optional.empty(), DocumentType.PUBLICATION));
    }

    public List<de.samply.frontend.dto.ProjectDocument> fetchOtherDocuments(Project project, Optional<ProjectBridgehead> bridgehead) {
        return convertToDto(documentService.fetchDocuments(project, bridgehead, DocumentType.OTHERS));
    }

    public Optional<ProjectDocument> fetchLastDocumentOfThisTypeForFrontend(Project project, Optional<ProjectBridgehead> bridgeheadOptional, DocumentType type) {
        return documentService
                .fetchLastDocumentOfThisType(project, bridgeheadOptional, type)
                .map(dtoFactory::convert);
    }

    private List<ProjectDocument> convertToDto(List<de.samply.db.model.ProjectDocument> projectDocumentList) {
        return projectDocumentList.stream().map(dtoFactory::convert).toList();
    }


}
