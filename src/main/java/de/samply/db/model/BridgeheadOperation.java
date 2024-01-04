package de.samply.db.model;

import de.samply.bridgehead.BridgeheadOperationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Entity
@Table(name = "bridgehead_operation", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BridgeheadOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "bridgehead", nullable = false)
    private String bridgehead;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "http_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private HttpStatus httpStatus;

    @Column(name = "error")
    private String error;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private BridgeheadOperationType type;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

}
