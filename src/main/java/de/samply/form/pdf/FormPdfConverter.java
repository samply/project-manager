package de.samply.form.pdf;

import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import de.samply.app.ProjectManagerConst;
import de.samply.pdf.PdfConverter;
import de.samply.pdf.PdfConverterException;
import de.samply.utils.directory.ExistingDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Component
public class FormPdfConverter implements PdfConverter {

    private final String resourcesBaseUri;

    public FormPdfConverter(@Value(ProjectManagerConst.FORM_RESOURCES_DIRECTORY_SV) ExistingDirectory resourcesPath) {
        this.resourcesBaseUri = resourcesPath.path().toUri().toString();
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
        try (PdfBoxRenderer renderer = new PdfRendererBuilder()
                    .withHtmlContent(content, resourcesBaseUri)
                    .buildPdfRenderer();
             PDDocument document = renderer.createPDFKeepOpen()) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                // OpenHTMLtoPDF sets text-field values before positioning their widgets.
                // Refresh once all rectangles exist so populated fields always get an
                // appearance stream and render consistently in every PDF viewer.
                acroForm.refreshAppearances();
            }
            document.save(outputStream);
        } catch (IOException e) {
            throw new PdfConverterException(e);
        }

    }

}
