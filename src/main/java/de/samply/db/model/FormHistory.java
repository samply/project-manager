package de.samply.db.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One recorded version of a form's configuration: the form's whole JSON
 * (canonical form, see FormDefinitionFactory) and its checksum. Rows are only
 * inserted, never updated, so the table is the history of every form.
 */
@Entity
@Table(name = "form_history", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "form_title", nullable = false)
    private String formTitle;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "checksum", nullable = false)
    private String checksum;

    @Column(name = "definition", nullable = false)
    private String definition;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

}
