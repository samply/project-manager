package de.samply.db.model;

import de.samply.project.ProjectType;
import de.samply.query.QueryFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private Instant createdAt = Instant.now();

    @Column(name = "query", nullable = false)
    private String query;

    @Column(name = "human_readable")
    private String humanReadable;

    @Column(name = "query_format", nullable = false)
    @Enumerated(EnumType.STRING)
    private QueryFormat queryFormat;

    @Column(name = "label")
    private String label;

    @Column(name = "description")
    private String description;

    @Column(name = "explorer_url")
    private String explorerUrl;

    @Column(name = "context")
    private String context;

    @OneToMany(mappedBy = "query", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QueryOutput> outputs = new HashSet<>();

    @Transient
    public Set<ProjectType> fetchProjectTypes() {
        return outputs.stream().map(QueryOutput::getProjectType).collect(Collectors.toSet());
    }

    @Transient
    public boolean hasProjectType(ProjectType projectType) {
        return outputs.stream().anyMatch(queryOutput -> queryOutput.getProjectType() == projectType);
    }

    @Transient
    public Optional<QueryOutput> fetchOutput(ProjectType projectType) {
        return outputs.stream()
                .filter(queryOutput -> queryOutput.getProjectType() == projectType).findFirst();
    }

    @Transient
    public void addOutput(QueryOutput output) {
        outputs.add(output);
        output.setQuery(this);
    }

    @Transient
    public void removeOutput(ProjectType projectType) {
        outputs.removeIf(queryOutput -> queryOutput.getProjectType() == projectType);
    }

}
