package de.samply.db.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "project_form_field", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "form_title", nullable = false)
    private String formTitle;

    @Column(name = "value")
    private String value;

    /**
     * Index of the block instance this field belongs to.
     * <p>
     * A block represents a repeatable set of form fields (e.g., var1, var2).
     * Each occurrence of the block is identified by a 1-based block instance number.
     * <p>
     * Example:
     * Block definition: (var1, var2)
     * Instances: 1, 2, 3
     * <p>
     * Then:
     * var1 | blockInstance = 1
     * var2 | blockInstance = 1
     * var1 | blockInstance = 2
     * var2 | blockInstance = 2
     */
    @Column(name = "block_instance")
    private Integer blockInstance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "modified_at")
    private Instant modifiedAt;


}
