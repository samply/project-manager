package de.samply.db.model;

import de.samply.document.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "project_document", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "url")
    private String url;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "bridgehead")
    private String bridgehead;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "creator_email", nullable = false)
    private String creatorEmail;

    @Column(name = "label")
    private String label;


}
