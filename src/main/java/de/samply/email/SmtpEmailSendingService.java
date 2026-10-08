package de.samply.email;

import de.samply.annotations.ConditionalOnModule;
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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Sends the emails through SMTP (test domains through the test mail sender, if configured), with their attachments,
 * and notes each one in the project's notifications. Part of the optional module EMAILS; the rendering stays in
 * {@link EmailService}.
 */
@Service
@Slf4j
@ConditionalOnModule(OptionalModule.EMAILS)
// Only for the IDE: at runtime only this or DisabledEmailSendingService exists (module condition). IntelliJ does not
// evaluate the condition and would report two candidates for EmailSendingService.
@Primary
public class SmtpEmailSendingService implements EmailSendingService {

    private final String emailFrom;
    private final JavaMailSender mailSender;
    private final Optional<JavaMailSender> testMailSender;
    private final EmailTemplates emailTemplates;
    private final EmailKeyValuesFactory emailKeyValuesFactory;
    private final List<String> testMailDomains;

    // Services
    private final EmailService emailService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AttachmentFileService attachmentFileService;


    public SmtpEmailSendingService(
            @Value(ProjectManagerConst.PROJECT_MANAGER_EMAIL_FROM_SV) String emailFrom,
            @Qualifier(ProjectManagerConst.PRIMARY_MAIL_SENDER) JavaMailSender mailSender,
            @Qualifier(ProjectManagerConst.TEST_MAIL_SENDER) Optional<JavaMailSender> testMailSender,
            EmailTemplates emailTemplates,
            EmailKeyValuesFactory emailKeyValuesFactory,
            @Value(ProjectManagerConst.TEST_EMAIL_DOMAINS_SV) List<String> testMailDomains,
            EmailService emailService,
            UserService userService,
            NotificationService notificationService,
            AttachmentFileService attachmentFileService) {
        this.emailFrom = emailFrom;
        this.mailSender = mailSender;
        this.testMailSender = testMailSender;
        this.emailTemplates = emailTemplates;
        this.emailKeyValuesFactory = emailKeyValuesFactory;
        this.testMailDomains = testMailDomains;
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
                sendEmail(emailTo, messageSubject.get(), attachments);
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

    private void sendEmail(String emailTo, MessageSubject messageSubject, List<FilenameAndFileContent> attachments) {
        try {
            fetchMailSender(emailTo).send(createMimeMessage(emailTo, emailFrom, messageSubject, attachments));
        } catch (MailException | EmailServiceException e) {
            log.error("Failed to send email");
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private MimeMessage createMimeMessage(
            String emailTo,
            String emailFrom,
            MessageSubject messageSubject,
            List<FilenameAndFileContent> attachments) throws EmailServiceException {
        try {
            return createMimeMessageWithoutHandlingException(emailTo, emailFrom, messageSubject, attachments);
        } catch (MessagingException e) {
            throw new EmailServiceException(e);
        }
    }

    private MimeMessage createMimeMessageWithoutHandlingException(
            String emailTo,
            String emailFrom,
            MessageSubject messageSubject,
            List<FilenameAndFileContent> attachments
    ) throws MessagingException {

        MimeMessage message = fetchMailSender(emailTo).createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                !attachments.isEmpty(), // multipart only if needed
                StandardCharsets.UTF_8.name()
        );

        helper.setTo(emailTo);
        helper.setFrom(emailFrom);
        helper.setSubject(messageSubject.subject());

        // Use helper, not message.setContent(...)
        helper.setText(
                messageSubject.message(),
                true // HTML
        );

        for (FilenameAndFileContent attachment : attachments) {
            helper.addAttachment(
                    attachment.filename(),
                    new ByteArrayResource(attachment.fileContent())
            );
        }

        return message;

    }

    private JavaMailSender fetchMailSender(@NotNull String emailTo) {
        return testMailSender.isPresent() && isTestMailDomain(emailTo) ? testMailSender.get() : mailSender;
    }

    private boolean isTestMailDomain(@NotNull String emailTo) {
        for (String domain : testMailDomains) {
            if (emailTo.contains(domain)) {
                return true;
            }
        }
        return false;
    }

}
