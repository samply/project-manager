package de.samply.db.model;


import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.UserProjectState;
import de.samply.query.QueryState;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "project_bridgehead", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectBridgehead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "bridgehead", nullable = false)
    private String bridgehead;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectBridgeheadState state = ProjectBridgeheadState.CREATED;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt = Instant.now();

    @Column(name = "results_url")
    @Convert(converter = EncryptionConverter.class)
    private String resultsUrl;

    @Column(name = "creator_results_state", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserProjectState creatorResultsState = UserProjectState.CREATED;

    @OneToMany(mappedBy = "projectBridgehead",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @ToString.Exclude
    private Set<ProjectBridgeheadExecution> executions = new HashSet<>();

    @Transient
    public void addOrUpdateExecution(@NotNull ProjectType projectType,
                                     QueryState queryState,
                                     String exporterResponse,
                                     String exporterUser,
                                     String exporterExecutionId,
                                     Integer exporterDispatchCounter) {

        this.project.fetchOutput(projectType).ifPresentOrElse(
                queryOutput -> {
                    ProjectBridgeheadExecution execution = executions.stream()
                            .filter(e -> e.getQueryOutput().equals(queryOutput))
                            .findFirst()
                            .orElseGet(() -> {
                                ProjectBridgeheadExecution newExec = new ProjectBridgeheadExecution();
                                newExec.setProjectBridgehead(this);
                                newExec.setQueryOutput(queryOutput);
                                executions.add(newExec);
                                return newExec;
                            });

                    // Update all provided fields
                    execution.setQueryState(queryState);
                    execution.setExporterResponse(exporterResponse);
                    execution.setExporterUser(exporterUser);
                    execution.setExporterExecutionId(exporterExecutionId);
                    if (exporterDispatchCounter != null) {
                        execution.setExporterDispatchCounter(exporterDispatchCounter);
                    }
                    execution.setModifiedAt(Instant.now());
                },
                () -> {
                    throw new IllegalArgumentException("ProjectCode does not have output for project type " + projectType);
                }
        );

    }

    @Transient
    public Optional<ProjectBridgeheadExecution> fetchExecution(ProjectType projectType) {
        return executions.stream()
                .filter(e -> e.getQueryOutput().getProjectType().equals(projectType))
                .findFirst();
    }

}
