package de.samply.form.template;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;

/**
 * Configured texts (display names, descriptions) may contain HTML: the frontend shows them with v-html. The thymeleaf
 * templates for the forms use this helper, available to them as {@code html}, to show them the same way:
 * <pre>
 *   &lt;div th:utext="${html.xhtml(field.labelDisplayName)}"&gt;&lt;/div&gt;
 *   &lt;textarea th:text="${html.text(field.fetchDisplayValue)}"&gt;&lt;/textarea&gt;
 * </pre>
 * The generated page is read as strict XML when it becomes a PDF. HTML that a browser accepts but XML does not (an
 * unclosed {@code <br>}, {@code &nbsp;}, a bare {@code &} or {@code <}) would make the whole PDF fail, so a text must
 * never go into th:utext as it is.
 */
public final class FormTemplateHtml {

    // What a text may contain: formatting, links, lists, tables, headings. No images (the PDF would have to fetch
    // them), no scripts, no styles. Links keep only their address: target and rel mean nothing in a PDF.
    private static final Safelist ALLOWED = Safelist.relaxed()
            .removeTags("img")
            .addProtocols("a", "href", "tel");

    private static final Document.OutputSettings XHTML = new Document.OutputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .charset("UTF-8")
            .prettyPrint(false);

    /**
     * The text as well-formed XHTML, for th:utext: its HTML is kept, and everything else is escaped.
     * Null stays null, so that th:if on the text still works.
     */
    public String xhtml(String text) {
        return text == null ? null : Jsoup.clean(text, "", ALLOWED, XHTML);
    }

    /**
     * The text without its HTML, for places that cannot show any: the value of a form widget (textarea, input).
     * A text without HTML is returned as it is, with its line breaks.
     */
    public String text(String text) {
        if (text == null || (text.indexOf('<') < 0 && text.indexOf('&') < 0)) {
            return text;
        }
        return Jsoup.parseBodyFragment(text).text();
    }

}
