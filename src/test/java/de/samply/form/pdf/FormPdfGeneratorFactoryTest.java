package de.samply.form.pdf;

import de.samply.pdf.PdfGenerator;
import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormPdfGeneratorFactoryTest {

    @Test
    void resolvesTemplateRelativeToConfiguredDirectory(@TempDir Path templateDirectory)
            throws Exception {
        Files.writeString(
                templateDirectory.resolve("request.html"),
                "<html><body><span th:text=\"${projectCode}\"></span></body></html>");
        FormPdfConverter converter = mock(FormPdfConverter.class);
        byte[] expectedPdf = {1, 2, 3};
        when(converter.convert(anyString())).thenReturn(expectedPdf);
        PdfGenerator generator = new FormPdfGeneratorFactory(
                converter, new ExistingDirectory(templateDirectory)).createPdfGenerator();

        byte[] pdf = generator.generatePdf("request", Map.of("projectCode", "ABC-123"));

        assertThat(pdf).isEqualTo(expectedPdf);
        ArgumentCaptor<String> generatedHtml = ArgumentCaptor.forClass(String.class);
        verify(converter).convert(generatedHtml.capture());
        assertThat(generatedHtml.getValue()).contains("ABC-123");
    }
}
