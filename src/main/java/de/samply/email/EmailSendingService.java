package de.samply.email;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

/**
 * Sends an email rendered from a template. Part of the optional module EMAILS: {@link SmtpEmailSendingService} when
 * enabled, {@link DisabledEmailSendingService} when not.
 */
public interface EmailSendingService {

    void sendEmail(@NotNull String emailTo, Optional<Project> project, Optional<ProjectBridgehead> bridgehead,
                   @NotNull ProjectRole role, @NotNull EmailTemplateType type) throws EmailServiceException;

    void sendEmail(@NotNull String emailTo, Optional<Project> project, Optional<ProjectBridgehead> bridgehead,
                   @NotNull ProjectRole role, @NotNull EmailTemplateType type, EmailKeyValues keyValues)
            throws EmailServiceException;

}
