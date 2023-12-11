package de.samply.db.model;

import de.samply.project.state.ProjectState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "project", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "state_machine_key", nullable = false)
    private String stateMachineKey;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "creator_email", nullable = false)
    private String creatorEmail;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "archived_at")
    private LocalDate archivedAt;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectState state;

    @ManyToOne
    @JoinColumn(name = "query_id")
    private Query query;

}
