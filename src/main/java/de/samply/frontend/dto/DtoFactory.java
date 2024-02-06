package de.samply.frontend.dto;

import de.samply.db.model.NotificationUserAction;
import jakarta.validation.constraints.NotNull;

import java.util.function.Supplier;

public class DtoFactory {

    public static Project convert(@NotNull de.samply.db.model.Project project) {
        return new Project(
                project.getCode(),
                project.getCreatorEmail(),
                project.getCreatedAt(),
                project.getExpiresAt(),
                project.getArchivedAt(),
                project.getModifiedAt(),
                project.getState(),
                project.getType(),
                project.getQuery().getQuery(),
                project.getQuery().getHumanReadable(),
                project.getQuery().getQueryFormat(),
                project.getQuery().getOutputFormat(),
                project.getQuery().getTemplateId(),
                project.getQuery().getLabel(),
                project.getQuery().getDescription(),
                project.getQuery().getExplorerUrl(),
                project.getQuery().getContext()
        );
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

    public static ProjectBridgehead convert(@NotNull de.samply.db.model.ProjectBridgehead projectBridgehead) {
        return new ProjectBridgehead(
                projectBridgehead.getProject().getCode(),
                projectBridgehead.getBridgehead(),
                projectBridgehead.getState(),
                projectBridgehead.getModifiedAt()
        );
    }

}
