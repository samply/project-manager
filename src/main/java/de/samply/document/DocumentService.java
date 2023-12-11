package de.samply.document;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectDocument;
import de.samply.db.repository.ProjectDocumentRepository;
import de.samply.db.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectRepository projectRepository;
    private final Path documentsDirectory;
    private final String timestampFormat;

    public DocumentService(ProjectDocumentRepository projectDocumentRepository,
                           ProjectRepository projectRepository,
                           @Value(ProjectManagerConst.PROJECT_DOCUMENTS_DIRECTORY_SV) String documentsDirectory,
                           @Value(ProjectManagerConst.PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT_SV) String timestampFormat) throws IOException {
        this.projectDocumentRepository = projectDocumentRepository;
        this.projectRepository = projectRepository;
        this.documentsDirectory = fetchPathDirectory(documentsDirectory);
        this.timestampFormat = timestampFormat;
    }

    public void uploadDocument(String projectCode, MultipartFile document) throws DocumentServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new DocumentServiceException("Project not found");
        }
        String originalFilename = document.getOriginalFilename();
        ProjectDocument projectDocument = null;
        if (originalFilename != null && !originalFilename.trim().isEmpty()) {
            Optional<ProjectDocument> projectDocumentOptional = this.projectDocumentRepository.findFirstByProjectAndAndOriginalFilename(project.get(), originalFilename);
            if (projectDocumentOptional.isPresent()) {
                projectDocument = projectDocumentOptional.get();
                deleteFile(projectDocument);
            }
        }
        if (projectDocument == null) {
            projectDocument = new ProjectDocument();
            projectDocument.setProject(project.get());
            projectDocument.setCreatedAt(LocalDate.now());
            projectDocument.setOriginalFilename(originalFilename);
        }
        Path documentPath = writeDocumentInDirectory(document);
        projectDocument.setFilePath(documentPath.toAbsolutePath().toString());
        this.projectDocumentRepository.save(projectDocument);
    }

    public void addDocumentUrl(String projectCode, String url) throws DocumentServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new DocumentServiceException("Project not found");
        }
        ProjectDocument projectDocument = new ProjectDocument();
        projectDocument.setProject(project.get());
        projectDocument.setUrl(url);
        projectDocument.setCreatedAt(LocalDate.now());
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

    public Optional<ProjectDocument> fetchProjectDocument(String projectCode, String filename) {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            return Optional.empty();
        }
        Optional<ProjectDocument> projectDocument = projectDocumentRepository.findFirstByProjectAndAndOriginalFilename(project.get(), filename);
        if (projectDocument.isEmpty()) {
            return Optional.empty();
        }
        projectDocument.get().setOriginalFilename(encodeFilename(filename));
        return projectDocument;
    }

    private String encodeFilename(String filename) {
        try {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return filename;
        }
    }

}
