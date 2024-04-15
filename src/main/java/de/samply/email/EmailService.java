package de.samply.email;

import de.samply.app.ProjectManagerConst;
import de.samply.frontend.FrontendService;
import de.samply.notification.NotificationService;
import de.samply.user.roles.ProjectRole;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class EmailService {

    private final String emailFrom;
    private final boolean enableEmails;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailTemplates emailTemplates;
    private final EmailContext emailContext;
    private final NotificationService notificationService;
    private final FrontendService frontendService;


    public EmailService(
            @Value(ProjectManagerConst.ENABLE_EMAILS_SV) Boolean enableEmails,
            @Value(ProjectManagerConst.PROJECT_MANAGER_EMAIL_FROM_SV) String emailFrom,
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            EmailTemplates emailTemplates,
            EmailContext emailContext,
            NotificationService notificationService,
            FrontendService frontendService) {
        this.emailFrom = emailFrom;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailTemplates = emailTemplates;
        this.emailContext = emailContext;
        this.enableEmails = enableEmails;
        this.notificationService = notificationService;
        this.frontendService = frontendService;
    }

    public void sendEmail(@NotNull String email, Optional<String> project, Optional<String> bridgehead, @NotNull ProjectRole role, @NotNull EmailTemplateType type) throws EmailServiceException {
        if (enableEmails) {
            sendEmail(email, project, bridgehead, role, type, new HashMap<>());
        } else {
            log.info("SMTP Server not enabled. Email to " + email + " with role " + role + " for bridgehead " +
                    (bridgehead.isPresent() ? bridgehead.get() : "NONE") + " and type " + type + " could not be sent");
        }
    }

    public void sendEmail(@NotNull String email, Optional<String> project, Optional<String> bridgehead, @NotNull ProjectRole role, @NotNull EmailTemplateType type, Map<String, String> keyValues) throws EmailServiceException {
        Map<String, String> context = new HashMap<>();
        project.ifPresent(p -> {
            context.put(ProjectManagerConst.EMAIL_CONTEXT_PROJECT_CODE, p);
            context.put(ProjectManagerConst.EMAIL_CONTEXT_PROJECT_VIEW_URL,
                    this.frontendService.fetchUrl(ProjectManagerConst.PROJECT_VIEW_SITE, Map.of(ProjectManagerConst.PROJECT_CODE, p)));
        });
        bridgehead.ifPresent(b -> context.put(ProjectManagerConst.EMAIL_CONTEXT_BRIDGEHEAD, b));
        context.putAll(keyValues);
        context.putAll(emailContext.getKeyValues());
        Optional<MessageSubject> messageSubject = createEmailMessageAndSubject(role, type, context);
        if (messageSubject.isPresent()) {
            mailSender.send(createMimeMessage(email, emailFrom, messageSubject.get()));
            // TODO: Add notification
        } else {
            throw new EmailServiceException("Template not found for " + type.name() + " of role " + role.name());
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
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);
        messageHelper.setTo(emailTo);
        message.setFrom(emailFrom);
        message.setSubject(messageSubject.subject());
        message.setText(messageSubject.message());
        return message;
    }

    private Optional<MessageSubject> createEmailMessageAndSubject(ProjectRole role, EmailTemplateType type, Map<String, String> keyValues) {
        Optional<TemplateSubject> template = emailTemplates.getTemplateAndSubject(type, role);
        if (template.isPresent()) {
            String message = templateEngine.process(template.get().template(), createContext(keyValues));
            return Optional.of(new MessageSubject(message, template.get().subject()));
        }
        return Optional.empty();
    }

    private String createEmailMessageAndSubject(String template, Map<String, String> keyValues) {
        return templateEngine.process(template, createContext(keyValues));
    }

    private Context createContext(Map<String, String> keyValues) {
        Context context = new Context();
        keyValues.forEach((key, value) -> context.setVariable(key, value));
        return context;
    }


}
