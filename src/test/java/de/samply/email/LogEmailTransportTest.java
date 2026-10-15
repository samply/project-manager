package de.samply.email;

import de.samply.email.attachment.FilenameAndFileContent;
import de.samply.user.roles.ProjectRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogEmailTransportTest {

    private static final MessageSubject MESSAGE = new MessageSubject(
            "<html><body><p>Dear researcher,</p><p>your request <b>REQ-2026-0001</b> was accepted.<br>Best regards</p>"
                    + "<ul><li>Site A</li><li>Site B</li></ul></body></html>",
            "Request accepted");

    @Test
    void summaryNamesTheReceiverAndTheKindOfEmail() {
        assertThat(new LogEmailTransport("summary").format("researcher@example.org", ProjectRole.CREATOR,
                EmailTemplateType.values()[0], MESSAGE, List.of()))
                .isEqualTo("Test email (not sent) to researcher@example.org (CREATOR): " + EmailTemplateType.values()[0]);
    }

    @Test
    void fullShowsSubjectAttachmentsAndTheTextOfTheEmail() {
        String log = new LogEmailTransport("FULL").format("researcher@example.org", ProjectRole.CREATOR,
                EmailTemplateType.values()[0], MESSAGE, List.of(new FilenameAndFileContent("request.pdf", new byte[0])));

        assertThat(log).contains("Test email (not sent) to researcher@example.org (CREATOR)")
                .contains("  Subject: Request accepted")
                .contains("  Attachments: request.pdf")
                .contains("  | Dear researcher,")
                .contains("  | your request REQ-2026-0001 was accepted.")
                .contains("  | Best regards")
                .contains("  | Site A")
                .contains("  | Site B")
                .doesNotContain("<");
    }

    @Test
    void stopsTheStartOnAnInvalidDetail() {
        assertThatThrownBy(() -> new LogEmailTransport("all"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EMAILS_TEST_LOG=all is not valid; allowed: summary, full");
    }

}
