package de.samply.db.model;

import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;
import de.samply.query.QueryProcess;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_query", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "query", nullable = false)
    private String query;

    @Column(name = "query_format", nullable = false)
    @Enumerated(EnumType.STRING)
    private QueryFormat queryFormat;

    @Column(name = "output_format", nullable = false)
    @Enumerated(EnumType.STRING)
    private OutputFormat outputFormat;

    @Column(name = "template_id", nullable = false)
    private String templateId;

    @Column(name = "label")
    private String label;

    @Column(name = "description")
    private String description;

    @Column(name = "query_proecess", nullable = false)
    @Enumerated(EnumType.STRING)
    private QueryProcess queryProcess;

}
