package de.samply.db.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_bridgehead_datashield", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectBridgeheadDataShield {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "project_bridgehead_id", nullable = false)
    private ProjectBridgehead projectBridgehead;

    @Column(name = "removed", nullable = false)
    private boolean removed = false;

}
