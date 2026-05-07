package de.samply.db.model;

import de.samply.query.QueryState;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "project_bridgehead_execution",
        schema = "samply",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"project_bridgehead_id", "query_output_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectBridgeheadExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_bridgehead_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Include
    private ProjectBridgehead projectBridgehead;

    @ManyToOne
    @JoinColumn(name = "query_output_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Include
    private QueryOutput queryOutput;

    @Column(name = "query_state", nullable = false)
    @Enumerated(EnumType.STRING)
    private QueryState queryState = QueryState.CREATED;

    @Column(name = "exporter_response")
    private String exporterResponse;

    @Column(name = "exporter_user")
    private String exporterUser;

    @Column(name = "exporter_execution_id")
    private String exporterExecutionId;

    @Column(name = "exporter_dispatch_counter")
    private int exporterDispatchCounter = 0;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt = Instant.now();

}