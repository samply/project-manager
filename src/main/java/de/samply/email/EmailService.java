package de.samply.email;

import de.samply.app.ProjectManagerConst;
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
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailTemplates emailTemplates;
    private final EmailContext emailContext;


    public EmailService(
            @Value(ProjectManagerConst.PROJECT_MANAGER_EMAIL_FROM_SV) String emailFrom,
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            EmailTemplates emailTemplates, EmailContext emailContext) {
        this.emailFrom = emailFrom;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailTemplates = emailTemplates;
        this.emailContext = emailContext;
    }

    public void sendEmail(@NotNull String email, Optional<String> bridgehead, @NotNull ProjectRole role, @NotNull EmailTemplateType type) throws EmailServiceException {
        sendEmail(email, bridgehead, role, type, new HashMap<>());
    }

    public void sendEmail(@NotNull String email, Optional<String> bridgehead, @NotNull ProjectRole role, @NotNull EmailTemplateType type, Map<String, String> keyValues) throws EmailServiceException {
        Map<String, String> context = new HashMap<>();
        bridgehead.ifPresent(b -> context.put(ProjectManagerConst.EMAIL_CONTEXT_BRIDGEHEAD, b));
        context.putAll(keyValues);
        context.putAll(emailContext.getKeyValues());
        Optional<MessageSubject> messageSubject = createEmailMessageAndSubject(role, type, context);
        if (messageSubject.isPresent()) {
            mailSender.send(createMimeMessage(email, emailFrom, messageSubject.get()));
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
