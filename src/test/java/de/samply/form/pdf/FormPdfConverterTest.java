package de.samply.form.pdf;

import de.samply.utils.directory.ExistingDirectory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FormPdfConverterTest {

    @Test
    void createsAppearanceStreamForPopulatedTextArea(@TempDir Path resourcesDirectory)
            throws Exception {
        FormPdfConverter converter = new FormPdfConverter(new ExistingDirectory(resourcesDirectory));

        byte[] pdf = converter.convert("""
                <html><body>
                <form>
                  <textarea name="querydata_analysis_method"
                            style="display:block; width:300px; height:80px">Federated analysis</textarea>
                </form>
                </body></html>
                """);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDField field = document.getDocumentCatalog().getAcroForm()
                    .getField("querydata_analysis_method");

            assertThat(field).isNotNull();
            assertThat(field.getValueAsString()).isEqualTo("Federated analysis");
            assertThat(field.getWidgets()).hasSize(1);
            assertThat(field.getWidgets().getFirst().getRectangle()).isNotNull();
            PDAppearanceDictionary appearance = field.getWidgets().getFirst().getAppearance();
            assertThat(appearance).isNotNull();
            assertThat(appearance.getNormalAppearance()).isNotNull();
        }
    }
}
