package de.samply.db.model;


import de.samply.project.state.ProjectBridgeheadState;
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
    private ProjectBridgeheadState state;

    @Column(name = "modified_at")
    private Instant modifiedAt;

}
