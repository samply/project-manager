package de.samply.email;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.user.roles.ProjectRole;
import de.samply.utils.KeyTransformer;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Optional;

/**
 * Renders the email templates (message and subject). Always available, also without the optional module EMAILS: the
 * Credentials Sharing Tool shows rendered templates without sending them. Sending: {@link EmailSendingService}.
 */
@Service
public class EmailService {

    private final TemplateEngine templateEngine;
    private final EmailTemplates emailTemplates;
    private final EmailKeyValuesFactory emailKeyValuesFactory;


    public EmailService(
            TemplateEngine templateEngine,
            EmailTemplates emailTemplates,
            EmailKeyValuesFactory emailKeyValuesFactory) {
        this.templateEngine = templateEngine;
        this.emailTemplates = emailTemplates;
        this.emailKeyValuesFactory = emailKeyValuesFactory;
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

}
