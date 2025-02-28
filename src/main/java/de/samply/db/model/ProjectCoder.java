package de.samply.db.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "project_coder", schema = "samply")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCoder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "app_secret", nullable = false)
    private String appSecret;

    @ManyToOne
    @JoinColumn(name = "project_bridgehead_user_id", nullable = false)
    private ProjectBridgeheadUser projectBridgeheadUser;

    @Column(name = "export_transferred")
    private boolean isExportTransferred = false;

    @Column(name = "workspace_id")
    private String workspaceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "in_app_register")
    private boolean isInAppRegister = false;


}
