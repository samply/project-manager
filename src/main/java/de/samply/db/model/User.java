package de.samply.db.model;


import de.samply.user.OrganisationRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "bridgehead")
    private String bridgehead;

    @Column(name = "organisation_role", nullable = false)
    private OrganisationRole organisationRole;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "disabled_at", nullable = false)
    private LocalDate disabledAt;


}
