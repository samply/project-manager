package de.samply.db.model;

import de.samply.project.ProjectType;
import de.samply.query.OutputFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "query_output", schema = "samply",
        uniqueConstraints = @UniqueConstraint(columnNames = {"query_id", "project_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;

    @Column(name = "project_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectType projectType;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "output_format")
    @Enumerated(EnumType.STRING)
    private OutputFormat outputFormat;

}