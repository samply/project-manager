package de.samply.notification.smtp;

import de.samply.app.ProjectManagerConst;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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


    public EmailService(
            @Value(ProjectManagerConst.PROJECT_MANAGER_EMAIL_FROM_SV) String emailFrom,
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            EmailTemplates emailTemplates) {
        this.emailFrom = emailFrom;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailTemplates = emailTemplates;
    }

    public void sendEmail(@NotNull String email, @NotNull ProjectRole role, @NotNull EmailTemplateType type) throws EmailServiceException {
        sendEmail(email, role, type, new HashMap<>());
    }

    public void sendEmail(@NotNull String email, @NotNull ProjectRole role, @NotNull EmailTemplateType type, Map<String, String> keyValues) throws EmailServiceException {
        SimpleMailMessage message = new SimpleMailMessage();
        Optional<MessageSubject> messageSubject = createEmailMessageAndSubject(role, type, keyValues);
        if (messageSubject.isPresent()) {
            message.setTo(email);
            message.setFrom(emailFrom);
            message.setSubject(messageSubject.get().subject());
            message.setText(messageSubject.get().message());
            mailSender.send(message);
        } else {
            throw new EmailServiceException("Template not found for " + type.name() + " of role " + role.name());
        }

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
