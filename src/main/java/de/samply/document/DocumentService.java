package de.samply.document;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectDocument;
import de.samply.db.repository.ProjectDocumentRepository;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.security.SessionUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class DocumentService {

    private final NotificationService notificationService;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final Path documentsDirectory;
    private final String timestampFormat;
    private final SessionUser sessionUser;

    public DocumentService(NotificationService notificationService,
                           ProjectDocumentRepository projectDocumentRepository,
                           @Value(ProjectManagerConst.PROJECT_DOCUMENTS_DIRECTORY_SV) String documentsDirectory,
                           @Value(ProjectManagerConst.PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT_SV) String timestampFormat,
                           SessionUser sessionUser) throws IOException {
        this.notificationService = notificationService;
        this.projectDocumentRepository = projectDocumentRepository;
        this.documentsDirectory = fetchPathDirectory(documentsDirectory);
        this.timestampFormat = timestampFormat;
        this.sessionUser = sessionUser;
    }

    public void uploadDocument(Project projectCode, Optional<ProjectBridgehead> bridgeheadOptional, MultipartFile document, DocumentType documentType, Optional<String> labelOptional) throws DocumentServiceException {
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
            projectDocument.setOriginalFilename(Objects.requireNonNull(document.getOriginalFilename()).trim());
            Path documentPath = writeDocumentInDirectory(document);
            projectDocument.setFilePath(documentPath.toAbsolutePath().toString());
        };
        addDocument(projectCode, bridgehead, documentType, labelOptional, documentInitializer, documentSetter);
    }

    public void addDocumentUrl(Project project, Optional<ProjectBridgehead> bridgeheadOptional, String url, DocumentType documentType, Optional<String> labelOptional) throws DocumentServiceException {
        String bridgehead = fetchBridgeheadForSearch(bridgeheadOptional);
        FunctionWithException<Project, Optional<ProjectDocument>> documentInitializer = testProject -> this.projectDocumentRepository.findFirstByProjectAndBridgeheadAndOriginalFilename(testProject, bridgehead, url);
        ConsumerWithException<ProjectDocument> documentSetter = projectDocument -> projectDocument.setUrl(url);
        addDocument(project, bridgehead, documentType, labelOptional, documentInitializer, documentSetter);
    }


    private String fetchBridgeheadForSearch(Optional<ProjectBridgehead> bridgehead) {
        return bridgehead.map(ProjectBridgehead::getBridgehead).orElse(ProjectManagerConst.NO_BRIDGEHEAD);
    }

    private void addDocument(Project project,
                             String bridgehead,
                             DocumentType documentType,
                             Optional<String> labelOptional,
                             FunctionWithException<Project, Optional<ProjectDocument>> documentInitializer,
                             ConsumerWithException<ProjectDocument> documentSetter) throws DocumentServiceException {
        ProjectDocument projectDocument;
        Optional<ProjectDocument> projectDocumentOptional = documentInitializer.apply(project);
        if (projectDocumentOptional.isPresent()) {
            projectDocument = projectDocumentOptional.get();
        } else {
            projectDocument = new ProjectDocument();
            projectDocument.setProject(project);
            projectDocument.setBridgehead(bridgehead);
        }
        labelOptional.ifPresent(projectDocument::setLabel);
        projectDocument.setDocumentType(documentType);
        projectDocument.setCreatedAt(Instant.now());
        projectDocument.setCreatorEmail(sessionUser.getEmail());
        documentSetter.accept(projectDocument);
        this.projectDocumentRepository.save(projectDocument);
        this.notificationService.createNotification(project, bridgehead, sessionUser.getEmail(), OperationType.ADD_DOCUMENT,
                "Add document of type " + documentType + ": " + projectDocument.getOriginalFilename(), null, null);
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

    public Optional<ProjectDocument> fetchProjectDocument(Project project, Optional<ProjectBridgehead> bridgeheadOptional, String filename) {
        String bridgehead = fetchBridgeheadForSearch(bridgeheadOptional);
        Optional<ProjectDocument> projectDocument = projectDocumentRepository.findFirstByProjectAndBridgeheadAndOriginalFilename(project, bridgehead, filename);
        if (projectDocument.isEmpty()) {
            projectDocument = projectDocumentRepository.findFirstByProjectAndOriginalFilename(project, filename);
        }
        return projectDocument;
    }

    public List<ProjectDocument> fetchDocuments(Project project, Optional<ProjectBridgehead> bridgehead, DocumentType documentType) {
        return (bridgehead.isPresent()) ?
                projectDocumentRepository.findAllByBridgeheadAndProjectAndDocumentTypeOrderByLabelAsc(bridgehead.get().getBridgehead(), project, documentType) :
                projectDocumentRepository.findAllByProjectAndDocumentTypeOrderByLabelAsc(project, documentType);
    }


    private interface ConsumerWithException<T> {
        void accept(T t) throws DocumentServiceException;
    }

    private interface FunctionWithException<T, R> {
        R apply(T t) throws DocumentServiceException;
    }

    public Optional<ProjectDocument> fetchLastDocumentOfThisType(Project project, Optional<ProjectBridgehead> bridgeheadOptional, DocumentType type) {
        return projectDocumentRepository.findFirstByProjectAndDocumentTypeAndBridgeheadOrderByCreatedAtDesc(
                project, type, fetchBridgeheadForSearch(bridgeheadOptional));
    }

    public Optional<ProjectDocument> fetchDocumentOrderByCreatedAtDesc(Project project) {
        return projectDocumentRepository.findTopByProjectOrderByCreatedAtDesc(project);
    }

}
