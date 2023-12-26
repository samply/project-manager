package de.samply.db.model;

import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "query", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Query {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "query", nullable = false)
    private String query;

    @Column(name = "human_readable")
    private String humanReadable;

    @Column(name = "query_format", nullable = false)
    @Enumerated(EnumType.STRING)
    private QueryFormat queryFormat;

    @Column(name = "output_format")
    @Enumerated(EnumType.STRING)
    private OutputFormat outputFormat;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "label")
    private String label;

    @Column(name = "description")
    private String description;

    @Column(name = "explorer_url")
    private String explorerUrl;

}
