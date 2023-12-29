package de.samply.document;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectDocument;
import de.samply.db.repository.ProjectDocumentRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.security.SessionUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class DocumentService {

    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectRepository projectRepository;
    private final Path documentsDirectory;
    private final String timestampFormat;
    private final SessionUser sessionUser;

    public DocumentService(ProjectDocumentRepository projectDocumentRepository,
                           ProjectRepository projectRepository,
                           @Value(ProjectManagerConst.PROJECT_DOCUMENTS_DIRECTORY_SV) String documentsDirectory,
                           @Value(ProjectManagerConst.PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT_SV) String timestampFormat,
                           SessionUser sessionUser) throws IOException {
        this.projectDocumentRepository = projectDocumentRepository;
        this.projectRepository = projectRepository;
        this.documentsDirectory = fetchPathDirectory(documentsDirectory);
        this.timestampFormat = timestampFormat;
        this.sessionUser = sessionUser;
    }

    public void uploadDocument(String projectCode, Optional<String> bridgeheadOptional, MultipartFile document, DocumentType documentType, Optional<String> labelOptional) throws DocumentServiceException {
        String bridgehead = fetchBridgeheadForSearch(bridgeheadOptional);
        FunctionWithException<Project, Optional<ProjectDocument>> documentInitializer = project -> {
            String originalFilename = document.getOriginalFilename();
            if (originalFilename != null && !originalFilename.trim().isEmpty()) {
                Optional<ProjectDocument> projectDocumentOptional =
                        this.projectDocumentRepository.findFirstByProjectAndBridgeheadAndOriginalFilename(project, bridgehead, originalFilename.trim());
                if (projectDocumentOptional.isPresent()) {
                    deleteFile(projectDocumentOptional.get());
                }
                return projectDocumentOptional;
            }
            return Optional.empty();
        };
        ConsumerWithException<ProjectDocument> documentSetter = projectDocument -> {
            projectDocument.setOriginalFilename(document.getOriginalFilename().trim());
            Path documentPath = writeDocumentInDirectory(document);
            projectDocument.setFilePath(documentPath.toAbsolutePath().toString());
        };
        addDocument(projectCode, bridgehead, documentType, labelOptional, documentInitializer, documentSetter);
    }

    public void addDocumentUrl(String projectCode, Optional<String> bridgeheadOptional, String url, DocumentType documentType, Optional<String> labelOptional) throws DocumentServiceException {
        String bridgehead = fetchBridgeheadForSearch(bridgeheadOptional);
        FunctionWithException<Project, Optional<ProjectDocument>> documentInitializer = project -> {
            return this.projectDocumentRepository.findFirstByProjectAndBridgeheadAndOriginalFilename(project, bridgehead, url);
        };
        ConsumerWithException<ProjectDocument> documentSetter = projectDocument -> {
            projectDocument.setUrl(url);
        };
        addDocument(projectCode, bridgehead, documentType, labelOptional, documentInitializer, documentSetter);
    }


    private String fetchBridgeheadForSearch(Optional<String> bridgehead) {
        return (bridgehead.isPresent()) ? bridgehead.get() : ProjectManagerConst.NO_BRIDGEHEAD;
    }

    private void addDocument(String projectCode,
                             String bridgehead,
                             DocumentType documentType,
                             Optional<String> labelOptional,
                             FunctionWithException<Project, Optional<ProjectDocument>> documentInitializer,
                             ConsumerWithException<ProjectDocument> documentSetter) throws DocumentServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new DocumentServiceException("Project not found");
        }
        ProjectDocument projectDocument;
        Optional<ProjectDocument> projectDocumentOptional = documentInitializer.apply(project.get());
        if (projectDocumentOptional.isPresent()) {
            projectDocument = projectDocumentOptional.get();
        } else {
            projectDocument = new ProjectDocument();
            projectDocument.setProject(project.get());
            projectDocument.setBridgehead(bridgehead);
        }
        labelOptional.ifPresent(label -> projectDocument.setLabel(label));
        projectDocument.setDocumentType(documentType);
        projectDocument.setCreatedAt(Instant.now());
        projectDocument.setCreatorEmail(sessionUser.getEmail());
        documentSetter.accept(projectDocument);
        this.projectDocumentRepository.save(projectDocument);
    }

    private Path writeDocumentInDirectory(MultipartFile document) throws DocumentServiceException {
        try {
            Path result = fetchPath(document);
            Files.write(result, document.getBytes());
            return result;
        } catch (IOException e) {
            throw new DocumentServiceException(e);
        }
    }

    private Path fetchPath(MultipartFile document) {
        String filename = (document.getOriginalFilename() == null || document.getOriginalFilename().trim().isEmpty()) ?
                generateRandomFilename() : document.getOriginalFilename();
        return documentsDirectory.resolve(fetchCurrentTimestamp() + "-" + filename);
    }

    private String fetchCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(timestampFormat));
    }

    private String generateRandomFilename() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, ProjectManagerConst.RANDOM_FILENAME_SIZE);
    }

    private Path fetchPathDirectory(String directory) throws IOException {
        Path directoryPath = Path.of(directory);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }
        return directoryPath;
    }

    private void deleteFile(ProjectDocument projectDocument) throws DocumentServiceException {
        try {
            deleteFileWithoutExceptionHandling(projectDocument);
        } catch (IOException e) {
            throw new DocumentServiceException(e);
        }
    }

    private void deleteFileWithoutExceptionHandling(ProjectDocument projectDocument) throws IOException {
        if (projectDocument.getFilePath() != null && !projectDocument.getFilePath().trim().isEmpty()) {
            Path documentPath = Path.of(projectDocument.getFilePath());
            if (Files.exists(documentPath)) {
                Files.delete(documentPath);
            }
        }
    }

    public Optional<ProjectDocument> fetchProjectDocument(String projectCode, Optional<String> bridgeheadOptional, String filename) {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            return Optional.empty();
        }
        String bridgehead = fetchBridgeheadForSearch(bridgeheadOptional);
        Optional<ProjectDocument> projectDocument = projectDocumentRepository.findFirstByProjectAndBridgeheadAndOriginalFilename(project.get(), bridgehead, filename);
        if (projectDocument.isEmpty()) {
            projectDocument = projectDocumentRepository.findFirstByProjectAndOriginalFilename(project.get(), filename);
        }
        if (projectDocument.isEmpty()) {
            return Optional.empty();
        }
        return projectDocument;
    }

    private interface ConsumerWithException<T> {
        void accept(T t) throws DocumentServiceException;
    }

    private interface FunctionWithException<T, R> {
        R apply(T t) throws DocumentServiceException;
    }

}
