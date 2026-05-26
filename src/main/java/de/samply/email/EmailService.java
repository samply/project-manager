package de.samply.email;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.email.attachment.AttachmentFileService;
import de.samply.email.attachment.FilenameAndFileContent;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.user.UserService;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.KeyTransformer;
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
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EmailService {

    private final String emailFrom;
    private final boolean enableEmails;
    private final JavaMailSender mailSender;
    private final Optional<JavaMailSender> testMailSender;
    private final TemplateEngine templateEngine;
    private final EmailTemplates emailTemplates;
    private final EmailKeyValuesFactory emailKeyValuesFactory;
    private final List<String> testMailDomains;

    // Services
    private final UserService userService;
    private final NotificationService notificationService;
    private final AttachmentFileService attachmentFileService;


    public EmailService(
            @Value(ProjectManagerConst.ENABLE_EMAILS_SV) Boolean enableEmails,
            @Value(ProjectManagerConst.PROJECT_MANAGER_EMAIL_FROM_SV) String emailFrom,
            @Qualifier(ProjectManagerConst.PRIMARY_MAIL_SENDER) JavaMailSender mailSender,
            @Qualifier(ProjectManagerConst.TEST_MAIL_SENDER) Optional<JavaMailSender> testMailSender,
            TemplateEngine templateEngine,
            EmailTemplates emailTemplates,
            NotificationService notificationService,
            EmailKeyValuesFactory emailKeyValuesFactory,
            @Value(ProjectManagerConst.TEST_EMAIL_DOMAINS_SV) List<String> testMailDomains,
            UserService userService,
            AttachmentFileService attachmentFileService) {
        this.emailFrom = emailFrom;
        this.mailSender = mailSender;
        this.testMailSender = testMailSender;
        this.templateEngine = templateEngine;
        this.emailTemplates = emailTemplates;
        this.enableEmails = enableEmails;
        this.notificationService = notificationService;
        this.emailKeyValuesFactory = emailKeyValuesFactory;
        this.testMailDomains = testMailDomains;
        this.userService = userService;
        this.attachmentFileService = attachmentFileService;
    }

    @Async(ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR)
    public void sendEmail(@NotNull String emailTo, Optional<Project> project, Optional<ProjectBridgehead> bridgehead, @NotNull ProjectRole role, @NotNull EmailTemplateType type) throws EmailServiceException {
        sendEmail(emailTo, project, bridgehead, role, type, this.emailKeyValuesFactory.newInstance());
    }

    @Async(ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR)
    public void sendEmail(@NotNull String emailTo, Optional<Project> project,
                          Optional<ProjectBridgehead> bridgehead, @NotNull ProjectRole role,
                          @NotNull EmailTemplateType type, EmailKeyValues keyValues) throws EmailServiceException {
        if (enableEmails && !userService.isUserInMailingBlackList(emailTo)) {
            project.ifPresent(keyValues::addProject);
            bridgehead.ifPresent(keyValues::addBridgehead);
            Optional<MessageSubject> messageSubject = createEmailMessageAndSubject(role, type, keyValues);
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
            log.info(enableEmails ? "SMTP Server not enabled." : "User Email in mailing blacklist");
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

    public Optional<MessageSubject> createEmailMessageAndSubject(ProjectRole role, EmailTemplateType type, EmailKeyValues keyValues) {
        Optional<TemplateSubject> template = emailTemplates.getTemplateAndSubject(type, role);
        if (template.isPresent()) {
            String message = templateEngine.process(template.get().template(), createContext(keyValues));
            return Optional.of(new MessageSubject(message, keyValues.replaceHtmlVariables(template.get().subject())));
        }
        return Optional.empty();
    }

    public Optional<MessageSubject> createEmailMessageAndSubject(String emailTo, Optional<Project> project, Optional<ProjectBridgehead> bridgehead, ProjectRole projectRole, EmailTemplateType emailTemplateType) {
        return createEmailMessageAndSubject(
                projectRole,
                emailTemplateType,
                emailKeyValuesFactory
                        .newInstance()
                        .add(new EmailRecipient(emailTo, project, bridgehead, projectRole)));
    }

    private Context createContext(EmailKeyValues keyValues) {
        Context context = new Context();
        keyValues.getKeyValues().forEach(context::setVariable);
        // Remove hyphens ("-") and convert keys to camel case to ensure Thymeleaf can process variables correctly.
        // For example, "my-variable" -> "myVariable".
        // In Thymeleaf templates, we can use <my-variable/> to reference the variable directly.
        // However, when using the standard Thymeleaf processor, we need to use <span th:text="${myVariable}">
        // because Thymeleaf does not support hyphens ("-") in variable names (e.g., ${my-variable} is invalid).
        KeyTransformer.transformMapKeys(keyValues.getKeyValues()).forEach(context::setVariable);
        return context;
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
