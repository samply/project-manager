package de.samply.db.model;


import de.samply.project.state.ProjectBridgeheadState;
import de.samply.query.QueryState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "project_bridgehead", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectBridgehead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
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


}
