package de.samply.email;

import de.samply.app.ProjectManagerConst;
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
    private final NotificationService notificationService;
    private final EmailKeyValuesFactory emailKeyValuesFactory;
    private final List<String> testMailDomains;
    private final UserService userService;


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
            UserService userService) {
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
    }

    @Async(ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR)
    public void sendEmail(@NotNull String emailTo, Optional<String> project, Optional<String> bridgehead, @NotNull ProjectRole role, @NotNull EmailTemplateType type) throws EmailServiceException {
        sendEmail(emailTo, project, bridgehead, role, type, this.emailKeyValuesFactory.newInstance());
    }

    @Async(ProjectManagerConst.ASYNC_EMAIL_SENDER_EXECUTOR)
    public void sendEmail(@NotNull String emailTo, Optional<String> project, Optional<String> bridgehead, @NotNull ProjectRole role, @NotNull EmailTemplateType type, EmailKeyValues keyValues) throws EmailServiceException {
        if (enableEmails && !userService.isUserInMailingBlackList(emailTo)) {
            project.ifPresent(keyValues::addProjectCode);
            bridgehead.ifPresent(keyValues::addBridgehead);
            Optional<MessageSubject> messageSubject = createEmailMessageAndSubject(role, type, keyValues);
            if (messageSubject.isPresent()) {
                sendEmail(emailTo, messageSubject.get());
                if (project.isPresent()) {
                    String details = "Email to " + emailTo + " (" + role + ") of type " + type.toString();
                    String message = keyValues.getKeyValues().get(EmailContextKey.MESSAGE.getValue());
                    if (message != null) {
                        details += " : " + message;
                    }
                    notificationService.createNotification(project.get(), bridgehead.isPresent() ? bridgehead.get() : null,
                            ProjectManagerConst.EMAIL_SERVICE, OperationType.SEND_EMAIL, details, null, null);
                }
            } else {
                throw new EmailServiceException("Template not found for " + type.name() + " of role " + role.name());
            }
        } else {
            log.info(enableEmails ? "SMTP Server not enabled." : "User Email in mailing blacklist");
            log.info("Email to " + emailTo + " with role " + role + " for bridgehead " +
                    (bridgehead.isPresent() ? bridgehead.get() : "NONE") + " and type " + type + " could not be sent");
        }
    }

    private void sendEmail(String emailTo, MessageSubject messageSubject) {
        try {
            fetchMailSender(emailTo).send(createMimeMessage(emailTo, emailFrom, messageSubject));
        } catch (MailException | EmailServiceException e) {
            log.error("Failed to send email");
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private MimeMessage createMimeMessage(String emailTo, String emailFrom, MessageSubject messageSubject) throws EmailServiceException {
        try {
            return createMimeMessageWithoutHandlingException(emailTo, emailFrom, messageSubject);
        } catch (MessagingException e) {
            throw new EmailServiceException(e);
        }
    }

    private MimeMessage createMimeMessageWithoutHandlingException(String emailTo, String emailFrom, MessageSubject messageSubject) throws MessagingException {
        MimeMessage message = fetchMailSender(emailTo).createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        message.setHeader("Content-Type", "text/html; charset=UTF-8");
        messageHelper.setTo(emailTo);
        message.setFrom(emailFrom);
        message.setSubject(messageSubject.subject(), StandardCharsets.UTF_8.name());
        message.setContent(messageSubject.message(), "text/html; charset=UTF-8");
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

    public Optional<MessageSubject> createEmailMessageAndSubject(String emailTo, Optional<String> projectCode, Optional<String> bridgehead, ProjectRole projectRole, EmailTemplateType emailTemplateType) {
        return createEmailMessageAndSubject(projectRole, emailTemplateType, emailKeyValuesFactory.newInstance().add(new EmailRecipient(emailTo, projectCode, bridgehead, projectRole)));
    }

    private Context createContext(EmailKeyValues keyValues) {
        Context context = new Context();
        keyValues.getKeyValues().forEach((key, value) -> context.setVariable(key, value));
        // Remove hyphens ("-") and convert keys to camel case to ensure Thymeleaf can process variables correctly.
        // For example: "my-variable" -> "myVariable".
        // In Thymeleaf templates, we can use <my-variable/> to reference the variable directly.
        // However, when using the standard Thymeleaf processor, we need to use <span th:text="${myVariable}">
        // because Thymeleaf does not support hyphens ("-") in variable names (e.g., ${my-variable} is invalid).
        KeyTransformer.transformMapKeys(keyValues.getKeyValues()).forEach((key, value) -> context.setVariable(key, value));
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
