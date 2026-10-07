package de.samply.email;

import de.samply.annotations.ModuleComponent;
import de.samply.annotations.ModuleStandIn;
import de.samply.db.model.Project;
import de.samply.modules.OptionalModule;
import de.samply.user.roles.ProjectRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class EmailsModuleTest {

    @Test
    void sendingAndTheSmtpConfigurationBelongToTheEmailsModule() {
        Stream.of(SmtpEmailSendingService.class, MailSenderConfiguration.class)
                .forEach(type -> assertThat(type.getAnnotation(ModuleComponent.class).value())
                        .as(type.getSimpleName()).isEqualTo(OptionalModule.EMAILS));
        assertThat(DisabledEmailSendingService.class.getAnnotation(ModuleStandIn.class).value())
                .isEqualTo(OptionalModule.EMAILS);
    }

    @Test
    void renderingIsNotPartOfTheModule() {
        assertThat(EmailService.class.getAnnotation(ModuleComponent.class)).isNull();
    }

    @Test
    void standInOnlyLogsTheEmail(CapturedOutput output) {
        Project project = new Project();
        project.setCode("REQ-2026-0001");

        new DisabledEmailSendingService().sendEmail("researcher@example.org", Optional.of(project), Optional.empty(),
                ProjectRole.CREATOR, EmailTemplateType.values()[0]);

        assertThat(output).contains("Emails are disabled (ENABLE_EMAILS=false): email to researcher@example.org")
                .contains("REQ-2026-0001");
    }

}
