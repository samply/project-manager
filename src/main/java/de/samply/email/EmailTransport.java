package de.samply.email;

import de.samply.email.attachment.FilenameAndFileContent;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Delivers a rendered email: {@link SmtpEmailTransport} (EMAILS=true) or {@link LogEmailTransport} (EMAILS=test).
 * Errors are logged, not thrown: a failed email must not undo the action that sent it.
 */
public interface EmailTransport {

    void send(@NotNull String emailTo, @NotNull ProjectRole role, @NotNull EmailTemplateType type,
              @NotNull MessageSubject messageSubject, @NotNull List<FilenameAndFileContent> attachments);

}
