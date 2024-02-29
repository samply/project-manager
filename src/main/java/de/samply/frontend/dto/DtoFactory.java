package de.samply.frontend.dto;

import de.samply.db.model.NotificationUserAction;
import jakarta.validation.constraints.NotNull;

import java.util.function.Supplier;

public class DtoFactory {

    public static Project convert(@NotNull de.samply.db.model.Project project) {
        Project result = new Project();
        result.setCode(project.getCode());
        result.setCreatorEmail(project.getCreatorEmail());
        result.setCreatedAt(project.getCreatedAt());
        result.setExpiresAt(project.getExpiresAt());
        result.setArchivedAt(project.getArchivedAt());
        result.setModifiedAt(project.getModifiedAt());
        result.setState(project.getState());
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

    public static User convert(@NotNull de.samply.db.model.ProjectBridgeheadUser projectBridgeheadUser) {
        return new User(
                projectBridgeheadUser.getEmail(),
                projectBridgeheadUser.getProjectBridgehead().getBridgehead(),
                projectBridgeheadUser.getProjectRole(),
                projectBridgeheadUser.getProjectState()
        );
    }

}
