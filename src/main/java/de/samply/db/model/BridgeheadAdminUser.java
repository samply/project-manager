package de.samply.db.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bridgehead_admin_user", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BridgeheadAdminUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "bridgehead", nullable = false)
    private String bridgehead;

}
