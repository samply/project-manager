package de.samply.email;

import de.samply.email.attachment.AttachmentFileService;
import de.samply.notification.NotificationService;
import de.samply.user.UserService;
import de.samply.user.roles.ProjectRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TemplateEmailSendingServiceTest {

    // An email is sent only when its template is configured: no template, no email, no error
    @Test
    void anEmailWithoutTemplateIsNotSent() {
        EmailService emailService = mock(EmailService.class);
        when(emailService.createEmailMessageAndSubject(any(), any(), any(), any())).thenReturn(Optional.empty());
        EmailTransport emailTransport = mock(EmailTransport.class);
        NotificationService notificationService = mock(NotificationService.class);
        TemplateEmailSendingService service = new TemplateEmailSendingService(new EmailTemplates(),
                mock(EmailKeyValuesFactory.class), emailTransport, emailService, mock(UserService.class),
                notificationService, mock(AttachmentFileService.class));

        assertThatCode(() -> service.sendEmail("someone@example.org", Optional.empty(), Optional.empty(),
                ProjectRole.CREATOR, EmailTemplateType.NEW_LOCAL_APPROVAL, mock(EmailKeyValues.class)))
                .doesNotThrowAnyException();
        verifyNoInteractions(emailTransport, notificationService);
    }

}
