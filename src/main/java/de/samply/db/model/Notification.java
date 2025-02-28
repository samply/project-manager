package de.samply.db.model;

import de.samply.notification.OperationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Entity
@Table(name = "notification", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp = Instant.now();

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "bridgehead")
    private String bridgehead;

    @Column(name = "operation_type")
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    @Column(name = "details")
    private String details;

    @Column(name = "error")
    private String error;

    @Column(name = "http_status")
    @Enumerated(EnumType.STRING)
    private HttpStatus httpStatus;


}
