package de.samply.db.model;

import de.samply.project.ProjectType;
import de.samply.query.OutputFormat;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "query_output", schema = "samply",
        uniqueConstraints = @UniqueConstraint(columnNames = {"query_id", "project_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class QueryOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude // prevents recursion
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