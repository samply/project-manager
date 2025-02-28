package de.samply.db.model;


import de.samply.project.state.UserProjectState;
import de.samply.user.roles.ProjectRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    @Enumerated(EnumType.STRING)
    private ProjectRole projectRole;

    @Column(name = "project_state", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserProjectState projectState = UserProjectState.CREATED;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt = Instant.now();

}
