package de.samply.db.model;


import de.samply.project.ProjectBridgeheadState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_bridgehead", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class ProjectBridgehead {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "bridgehead", nullable = false)
    private String bridgehead;

    @Column(name = "state", nullable = false)
    private ProjectBridgeheadState state;

}
