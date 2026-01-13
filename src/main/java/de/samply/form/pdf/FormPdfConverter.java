package de.samply.form.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import de.samply.app.ProjectManagerConst;
import de.samply.pdf.PdfConverter;
import de.samply.pdf.PdfConverterException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;

@Component
public class FormPdfConverter implements PdfConverter {

    private final String resourcesBaseUri;

    public FormPdfConverter(@Value(ProjectManagerConst.FORM_RESOURCES_PATH_SV) String resourcesPath) {
        this.resourcesBaseUri = Paths.get(resourcesPath).toUri().toString();
    }

    @Override
    public byte[] convert(String content) throws PdfConverterException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            convertContentToPdfAndRenderInOutputStream(content, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new PdfConverterException(e);
        }
    }

    private void convertContentToPdfAndRenderInOutputStream(String content, OutputStream outputStream) throws PdfConverterException {
        try {
            new PdfRendererBuilder()
                    .withHtmlContent(content, resourcesBaseUri)
                    .toStream(outputStream)
                    .run();
        } catch (IOException e) {
            throw new PdfConverterException(e);
        }

    }

}
