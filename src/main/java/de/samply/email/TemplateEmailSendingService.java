package de.samply.email;

import de.samply.annotations.ConditionalOnModule;
import de.samply.annotations.ConditionalOnModuleTest;
import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.email.attachment.AttachmentFileService;
import de.samply.email.attachment.FilenameAndFileContent;
import de.samply.modules.OptionalModule;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.user.UserService;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Sends the emails rendered from their templates, with their attachments, and notes each one in the project's
 * notifications; the {@link EmailTransport} delivers them: through SMTP ({@link SmtpEmailTransport}), or in test mode
 * to the log ({@link LogEmailTransport}). Part of the optional module EMAILS; the rendering stays in
 * {@link EmailService}.
 */
@Service
@Slf4j
@ConditionalOnModule(OptionalModule.EMAILS)
// Also in test mode: everything runs as in production, only the transport writes the email to the log
@ConditionalOnModuleTest(OptionalModule.EMAILS)
// Only for the IDE: at runtime only this or DisabledEmailSendingService exists (module condition). IntelliJ does not
// evaluate the condition and would report two candidates for EmailSendingService.
@Primary
public class TemplateEmailSendingService implements EmailSendingService {

    private final EmailTemplates emailTemplates;
    private final EmailKeyValuesFactory emailKeyValuesFactory;
    private final EmailTransport emailTransport;

    // Services
    private final EmailService emailService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AttachmentFileService attachmentFileService;


    public TemplateEmailSendingService(
            EmailTemplates emailTemplates,
            EmailKeyValuesFactory emailKeyValuesFactory,
            EmailTransport emailTransport,
            EmailService emailService,
            UserService userService,
            NotificationService notificationService,
            AttachmentFileService attachmentFileService) {
        this.emailTemplates = emailTemplates;
        this.emailKeyValuesFactory = emailKeyValuesFactory;
        this.emailTransport = emailTransport;
        this.emailService = emailService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.attachmentFileService = attachmentFileService;
    }

    @Override
    @Async(ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR)
    public void sendEmail(@NotNull String emailTo, Optional<Project> project, Optional<ProjectBridgehead> bridgehead, @NotNull ProjectRole role, @NotNull EmailTemplateType type) throws EmailServiceException {
        sendEmail(emailTo, project, bridgehead, role, type, this.emailKeyValuesFactory.newInstance());
    }

    @Override
    @Async(ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR)
    public void sendEmail(@NotNull String emailTo, Optional<Project> project,
                          Optional<ProjectBridgehead> bridgehead, @NotNull ProjectRole role,
                          @NotNull EmailTemplateType type, EmailKeyValues keyValues) throws EmailServiceException {
        if (!userService.isUserInMailingBlackList(emailTo)) {
            project.ifPresent(keyValues::addProject);
            bridgehead.ifPresent(keyValues::addBridgehead);
            Optional<MessageSubject> messageSubject = emailService.createEmailMessageAndSubject(role, type, keyValues);
            if (messageSubject.isPresent()) {
                List<FilenameAndFileContent> attachments = fetchAttachments(project, type);
                emailTransport.send(emailTo, role, type, messageSubject.get(), attachments);
                if (project.isPresent()) {
                    String details = "Email to " + emailTo + " (" + role + ") of type " + type.toString();
                    String message = keyValues.getKeyValues().get(EmailContextKey.MESSAGE.getValue());
                    if (message != null) {
                        details += " : " + message;
                    }
                    notificationService.createNotification(project.get(), bridgehead.map(ProjectBridgehead::getBridgehead).orElse(null),
                            ProjectManagerConst.EMAIL_SERVICE, OperationType.SEND_EMAIL, details, null, null);
                }
            } else {
                throw new EmailServiceException("Template not found for " + type.name() + " of role " + role.name());
            }
        } else {
            log.info("User email in mailing blacklist");
            log.info("Email to {} with role {} for bridgehead {} and type {} could not be sent", emailTo, role,
                    bridgehead.map(ProjectBridgehead::getBridgehead).orElse("NONE"), type);
        }
    }

    private List<FilenameAndFileContent> fetchAttachments(Optional<Project> project, EmailTemplateType type) {
        return project
                .map(tempProject -> emailTemplates
                        .getAttachmentFiles(type)
                        .stream()
                        .flatMap(attachmentFile -> attachmentFileService
                                .fetchAttachmentFilenameAndContent(
                                        tempProject,
                                        attachmentFile,
                                        Optional.empty()
                                )
                                .stream()
                        )
                        .toList()
                )
                .orElseGet(List::of);
    }

}
