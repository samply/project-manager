package de.samply.form.pdf;

import de.samply.app.ProjectManagerConst;
import de.samply.form.FormService;
import de.samply.pdf.PdfGenerator;
import de.samply.pdf.PdfGeneratorException;
import de.samply.utils.LanguageUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class FormPdfService {

    private final FormService formService;
    private final PdfGenerator pdfGenerator;
    private final String defaultFormTemplate;
    private final String defaultLanguage;

    public FormPdfService(FormService formService,
                          FormPdfGeneratorFactory pdfGeneratorFactory,
                          @Value(ProjectManagerConst.FORM_DEFAULT_TEMPLATE_SV) String defaultFormTemplate,
                          @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage) {
        this.formService = formService;
        this.pdfGenerator = pdfGeneratorFactory.createPdfGenerator();
        this.defaultFormTemplate = defaultFormTemplate;
        this.defaultLanguage = defaultLanguage;
    }

    public String fetchFormFilename(@NotNull String projectCode, Optional<String> formTemplate, Optional<String> language) {
        //TODO
        return "form.pdf";
    }

    public byte[] createFormAsPdf(@NotNull String projectCode, Optional<String> formTemplate, Optional<String> language) throws FormPdfServiceException {
        try {
            return pdfGenerator.generatePdf(
                    formTemplate.orElse(defaultFormTemplate),
                    createContext(projectCode, LanguageUtils.normalize(language.orElse(defaultLanguage))));
        } catch (PdfGeneratorException e) {
            throw new FormPdfServiceException(e);
        }
    }

    private Map<String, Object> createContext(String projectCode, String language) {
        //TODO
        return Map.of();
    }

}
