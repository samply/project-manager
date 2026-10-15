package de.samply.email;

import de.samply.annotations.ConditionalOnModuleTest;
import de.samply.app.ProjectManagerConst;
import de.samply.email.attachment.FilenameAndFileContent;
import de.samply.modules.OptionalModule;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test mode of EMAILS (ENABLE_EMAILS=test): writes the email to the log instead of sending it. EMAILS_TEST_LOG chooses
 * how much: "summary" (receiver and kind of email, the default) or "full" (also subject, text and attachments).
 */
@Slf4j
@Component
@ConditionalOnModuleTest(OptionalModule.EMAILS)
public class LogEmailTransport implements EmailTransport {

    enum Detail {SUMMARY, FULL}

    private final Detail detail;

    public LogEmailTransport(@Value(ProjectManagerConst.EMAILS_TEST_LOG_SV) String detail) {
        this.detail = Arrays.stream(Detail.values())
                .filter(value -> value.name().equalsIgnoreCase(detail.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(ProjectManagerConst.EMAILS_TEST_LOG + "=" + detail
                        + " is not valid; allowed: summary, full"));
    }

    @Override
    public void send(@NotNull String emailTo, @NotNull ProjectRole role, @NotNull EmailTemplateType type,
                     @NotNull MessageSubject messageSubject, @NotNull List<FilenameAndFileContent> attachments) {
        log.info(format(emailTo, role, type, messageSubject, attachments));
    }

    String format(String emailTo, ProjectRole role, EmailTemplateType type, MessageSubject messageSubject,
                  List<FilenameAndFileContent> attachments) {
        String summary = "Test email (not sent) to " + emailTo + " (" + role + "): " + type;
        if (detail == Detail.SUMMARY) {
            return summary;
        }
        return summary + "\n"
                + "  Subject: " + messageSubject.subject() + "\n"
                + (attachments.isEmpty() ? "" : "  Attachments: " + attachments.stream()
                .map(FilenameAndFileContent::filename).collect(Collectors.joining(", ")) + "\n")
                + toText(messageSubject.message()).lines().map(line -> "  | " + line).collect(Collectors.joining("\n"));
    }

    // The HTML of the template as readable text: paragraphs, line breaks and list items on their own lines
    static String toText(String html) {
        Document document = Jsoup.parse(html == null ? "" : html);
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));
        document.select("br").after("\\n");
        document.select("p, div, li, tr, h1, h2, h3, h4, h5, h6").before("\\n");
        String text = Jsoup.clean(document.html(), "", Safelist.none(), new Document.OutputSettings().prettyPrint(false))
                .replace("\\n", "\n")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
        return text.lines()
                .map(String::strip)
                .collect(Collectors.joining("\n"))
                .replaceAll("\n{3,}", "\n\n")
                .strip();
    }

}
