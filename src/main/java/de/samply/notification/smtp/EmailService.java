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
    public void sendEmail(@NotNull String email, @NotNull ProjectRole role, @NotNull EmailTemplateType type){
        sendEmail(email, role, type, new HashMap<>());
    }

    public void sendEmail(@NotNull String email, @NotNull ProjectRole role, @NotNull EmailTemplateType type, Map<String, String> keyValues){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(emailFrom);
        message.setSubject("TODO: REPLACE ME"); //TODO
        message.setText(createEmailBody(role, type, keyValues));
        mailSender.send(message);
    }

    private String createEmailBody(ProjectRole role, EmailTemplateType type, Map<String, String> keyValues) {
        return templateEngine.process(emailTemplates.getTemplate(type, role).get(), createContext(keyValues));
    }

    private String createEmailBody(String template, Map<String, String> keyValues) {
        return templateEngine.process(template, createContext(keyValues));
    }

    private Context createContext(Map<String, String> keyValues) {
        Context context = new Context();
        keyValues.forEach((key, value) -> context.setVariable(key, value));
        return context;
    }


}
