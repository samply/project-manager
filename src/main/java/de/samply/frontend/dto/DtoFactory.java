package de.samply.frontend.dto;

import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.NotificationUserAction;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

@Component
public class DtoFactory {

    private BridgeheadConfiguration bridgeheadConfiguration;

    public DtoFactory(BridgeheadConfiguration bridgeheadConfiguration) {
        this.bridgeheadConfiguration = bridgeheadConfiguration;
    }

    public static Project convert(@NotNull de.samply.db.model.Project project) {
        Project result = new Project();
        result.setCode(project.getCode());
        result.setCreatorEmail(project.getCreatorEmail());
        result.setCreatedAt(project.getCreatedAt());
        result.setExpiresAt(project.getExpiresAt());
        result.setArchivedAt(project.getArchivedAt());
        result.setModifiedAt(project.getModifiedAt());
        result.setState(project.getState());
        result.setCustomConfig(project.isCustomConfig());
        result.setType(project.getType());
        result.setQuery(project.getQuery().getQuery());
        result.setHumanReadable(project.getQuery().getHumanReadable());
        result.setQueryFormat(project.getQuery().getQueryFormat());
        result.setOutputFormat(project.getQuery().getOutputFormat());
        result.setTemplateId(project.getQuery().getTemplateId());
        result.setLabel(project.getQuery().getLabel());
        result.setDescription(project.getQuery().getDescription());
        result.setExplorerUrl(project.getQuery().getExplorerUrl());
        result.setQueryContext(project.getQuery().getContext());
        return result;
    }

    public static de.samply.db.model.Project convert(@NotNull Project projectConfiguration, @NotNull de.samply.db.model.Project project) {
        if (projectConfiguration.getExpiresAt() != null) {
            project.setExpiresAt(projectConfiguration.getExpiresAt());
        }
        if (projectConfiguration.getType() != null) {
            project.setType(projectConfiguration.getType());
        }
        if (projectConfiguration.getQuery() != null) {
            project.getQuery().setQuery(projectConfiguration.getQuery());
        }
        if (projectConfiguration.getHumanReadable() != null) {
            project.getQuery().setHumanReadable(projectConfiguration.getHumanReadable());
        }
        if (projectConfiguration.getQueryFormat() != null) {
            project.getQuery().setQueryFormat(projectConfiguration.getQueryFormat());
        }
        if (projectConfiguration.getOutputFormat() != null) {
            project.getQuery().setOutputFormat(projectConfiguration.getOutputFormat());
        }
        if (projectConfiguration.getTemplateId() != null) {
            project.getQuery().setTemplateId(projectConfiguration.getTemplateId());
        }
        if (projectConfiguration.getQueryContext() != null) {
            project.getQuery().setContext(projectConfiguration.getQueryContext());
        }
        return project;
    }

    public static Notification convert(@NotNull de.samply.db.model.Notification notification, Supplier<NotificationUserAction> userActionSupplier) {
        return new Notification(
                notification.getId(),
                notification.getEmail(),
                notification.getTimestamp(),
                notification.getProject().getCode(),
                notification.getBridgehead(),
                notification.getOperationType(),
                notification.getDetails(),
                notification.getError(),
                notification.getHttpStatus(),
                userActionSupplier.get().isRead()
        );
    }

    public static ProjectDocument convert(@NotNull de.samply.db.model.ProjectDocument projectDocument) {
        return new ProjectDocument(
                projectDocument.getProject().getCode(),
                projectDocument.getOriginalFilename(),
                projectDocument.getUrl(),
                projectDocument.getCreatedAt(),
                projectDocument.getBridgehead(),
                projectDocument.getCreatorEmail(),
                projectDocument.getLabel(),
                projectDocument.getDocumentType()
        );
    }

    public ProjectBridgehead convert(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        return new ProjectBridgehead(
                projectBridgehead.getProject().getCode(),
                projectBridgehead.getBridgehead(),
                fetchHumanReadableBridgehead(projectBridgehead),
                projectBridgehead.getState(),
                projectBridgehead.getModifiedAt(),
                projectBridgehead.getQueryState()
        );
    }

    public String fetchHumanReadableBridgehead(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        Optional<String> humanReadable = bridgeheadConfiguration.getHumanReadable(projectBridgehead.getBridgehead());
        return (humanReadable.isPresent()) ? humanReadable.get() : projectBridgehead.getBridgehead();
    }

    public static User convert(@NotNull de.samply.db.model.ProjectBridgeheadUser projectBridgeheadUser) {
        return new User(
                projectBridgeheadUser.getEmail(),
                projectBridgeheadUser.getProjectBridgehead().getBridgehead(),
                projectBridgeheadUser.getProjectRole(),
                projectBridgeheadUser.getProjectState()
        );
    }

    public static User convertFilteringProjectRoleAndState(@NotNull de.samply.db.model.ProjectBridgeheadUser projectBridgeheadUser) {
        return new User(
                projectBridgeheadUser.getEmail(),
                projectBridgeheadUser.getProjectBridgehead().getBridgehead(),
                null,
                null
        );
    }

}
