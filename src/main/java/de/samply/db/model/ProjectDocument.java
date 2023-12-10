package de.samply.db.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "file-path")
    private String filePath;

    @Column(name = "url")
    private String url;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

}
