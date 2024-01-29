package de.samply.db.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_user_action", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationUserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "read", nullable = false)
    private boolean read = false;

    @ManyToOne
    @JoinColumn(name = "notification_id")
    private Notification notification;

}
