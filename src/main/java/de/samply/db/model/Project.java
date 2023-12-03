package de.samply.db.model;

import de.samply.project.ProjectState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "project", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "state_machine_key", nullable = false)
    private UUID stateMachineKey;

    @Column(name = "name")
    private String name;

    @Column(name = "contact_id")
    private String contactId;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "archived_at")
    private LocalDate archivedAt;

    @Column(name = "state")
    private ProjectState state;


}
