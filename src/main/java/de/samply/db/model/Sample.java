package de.samply.db.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sample", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "affiliation")
    private String affiliation;

    @Column(name = "study_objective")
    private String studyObjective;

    @Column(name = "method")
    private String method;

    @Column(name = "ethical_approval")
    private String ethicalApproval;

    @Column(name = "gratification")
    private String gratification;

    @Column(name = "donor")
    private String donor;

    @Column(name = "collection_description")
    private String collectionDescription;

    @Column(name = "type_and_quantity")
    private String typeAndQuantity;

    @Column(name = "matched_required")
    private Boolean matchedRequired = false;

    @Column(name = "slides_per_sample")
    private Integer slidesPerSample;

    @Column(name = "stained_slides")
    private Boolean stainedSlides = false;

    @Column(name = "slide_sufficiency_staining")
    private String slideSufficiencyStaining;

    @Column(name = "specific_requirements")
    private String specificRequirements;

}
