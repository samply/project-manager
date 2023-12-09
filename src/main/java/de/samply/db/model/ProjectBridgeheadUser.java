package de.samply.db.model;


import de.samply.user.roles.ProjectRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_bridgehead_user", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectBridgeheadUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_bridgehead_id", nullable = false)
    private ProjectBridgehead projectBridgehead;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "project_role", nullable = false)
    private ProjectRole projectRole;

}
