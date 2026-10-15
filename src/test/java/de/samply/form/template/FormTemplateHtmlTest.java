package de.samply.form.template;

import de.samply.form.template.pdf.FormPdfConverter;
import de.samply.utils.directory.ExistingDirectory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FormTemplateHtmlTest {

    private final FormTemplateHtml html = new FormTemplateHtml();

    @Test
    void keepsTheHtmlOfAText() {
        assertThat(html.xhtml("<strong>Please note:</strong> 600GB per case"))
                .isEqualTo("<strong>Please note:</strong> 600GB per case");
        assertThat(html.xhtml("<ul><li>one</li><li>two</li></ul>")).isEqualTo("<ul><li>one</li><li>two</li></ul>");
    }

    @Test
    void keepsTheAddressOfALinkAndDropsWhatAPdfCannotUse() {
        assertThat(html.xhtml("<a href=\"https://example.org/x\" target=\"_blank\" rel=\"noopener noreferrer\">the page</a>"))
                .isEqualTo("<a href=\"https://example.org/x\">the page</a>");
        assertThat(html.xhtml("<a href=\"mailto:someone@example.org\">someone@example.org</a>"))
                .isEqualTo("<a href=\"mailto:someone@example.org\">someone@example.org</a>");
    }

    @Test
    void turnsBrowserHtmlIntoWellFormedXhtml() {
        assertThat(html.xhtml("line one<br>line two")).isEqualTo("line one<br />line two");
        assertThat(html.xhtml("<b>bold")).isEqualTo("<b>bold</b>");
        assertThat(html.xhtml("a&nbsp;b")).isEqualTo("a&#xa0;b");
        assertThat(html.xhtml("<a href=\"https://example.org/?a=1&b=2\">q</a>"))
                .isEqualTo("<a href=\"https://example.org/?a=1&amp;b=2\">q</a>");
    }

    @Test
    void escapesTextThatOnlyLooksLikeHtml() {
        assertThat(html.xhtml("R & D")).isEqualTo("R &amp; D");
        assertThat(html.xhtml("a < b")).isEqualTo("a &lt; b");
        assertThat(html.xhtml("Größe in µl")).isEqualTo("Größe in µl");
    }

    @Test
    void removesWhatMustNotReachThePdf() {
        assertThat(html.xhtml("before<script>alert(1)</script>after")).isEqualTo("beforeafter");
        assertThat(html.xhtml("<img src=\"https://example.org/a.png\">text")).isEqualTo("text");
        assertThat(html.xhtml("<a href=\"javascript:alert(1)\">x</a>")).isEqualTo("<a>x</a>");
    }

    @Test
    void keepsAMissingTextMissing() {
        assertThat(html.xhtml(null)).isNull();
        assertThat(html.text(null)).isNull();
    }

    @Test
    void removesTheHtmlForAFormWidget() {
        assertThat(html.text("I agree to notify <a href=\"mailto:a@example.org\">a@example.org</a> in advance"))
                .isEqualTo("I agree to notify a@example.org in advance");
        assertThat(html.text("R &amp; D")).isEqualTo("R & D");
        // Without HTML the text is untouched, line breaks included
        assertThat(html.text("first line\nsecond line")).isEqualTo("first line\nsecond line");
    }

    // The generated page is read as strict XML: each of these texts, put into it as it is, makes the PDF fail.
    @ParameterizedTest
    @ValueSource(strings = {
            "line one<br>line two",
            "a&nbsp;b",
            "R & D",
            "a < b",
            "<b>bold",
            "<a href=\"https://example.org/?a=1&b=2\">q</a>",
            "see <a href=\"https://example.org/x\" target=\"_blank\" rel=\"noopener noreferrer\">the page</a>"
    })
    void aPageWithTheConvertedTextBecomesAPdf(String text, @TempDir Path resourcesDirectory) throws Exception {
        FormPdfConverter converter = new FormPdfConverter(new ExistingDirectory(resourcesDirectory));

        byte[] pdf = converter.convert("<html><body><div>" + html.xhtml(text) + "</div></body></html>");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            // The text is there, and none of its markup is printed
            assertThat(new PDFTextStripper().getText(document)).isNotBlank()
                    .doesNotContain("<a ", "<b>", "<br", "&nbsp;", "&amp;", "&lt;");
        }
    }

    @Test
    void aLinkIsClickableInThePdf(@TempDir Path resourcesDirectory) throws Exception {
        FormPdfConverter converter = new FormPdfConverter(new ExistingDirectory(resourcesDirectory));

        byte[] pdf = converter.convert("<html><body><div>"
                + html.xhtml("the <a href=\"https://example.org/x\" target=\"_blank\">Usage Agreement</a>")
                + "</div></body></html>");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document)).contains("the Usage Agreement");
            assertThat(document.getPage(0).getAnnotations()).hasSize(1).first().isInstanceOf(PDAnnotationLink.class);
        }
    }

}
