package de.samply.email;

import de.samply.annotations.ConditionalOnModule;
import de.samply.app.ProjectManagerConst;
import de.samply.email.attachment.FilenameAndFileContent;
import de.samply.modules.OptionalModule;
import de.samply.user.roles.ProjectRole;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Sends the emails through SMTP (test domains through the test mail sender, if configured).
 */
@Slf4j
@Component
@ConditionalOnModule(OptionalModule.EMAILS)
// Only for the IDE: at runtime only this or LogEmailTransport exists (module condition)
@Primary
public class SmtpEmailTransport implements EmailTransport {

    private final String emailFrom;
    private final JavaMailSender mailSender;
    private final Optional<JavaMailSender> testMailSender;
    private final List<String> testMailDomains;

    public SmtpEmailTransport(
            @Value(ProjectManagerConst.PROJECT_MANAGER_EMAIL_FROM_SV) String emailFrom,
            @Qualifier(ProjectManagerConst.PRIMARY_MAIL_SENDER) JavaMailSender mailSender,
            @Qualifier(ProjectManagerConst.TEST_MAIL_SENDER) Optional<JavaMailSender> testMailSender,
            @Value(ProjectManagerConst.TEST_EMAIL_DOMAINS_SV) List<String> testMailDomains) {
        this.emailFrom = emailFrom;
        this.mailSender = mailSender;
        this.testMailSender = testMailSender;
        this.testMailDomains = testMailDomains;
    }

    @Override
    public void send(@NotNull String emailTo, @NotNull ProjectRole role, @NotNull EmailTemplateType type,
                     @NotNull MessageSubject messageSubject, @NotNull List<FilenameAndFileContent> attachments) {
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
