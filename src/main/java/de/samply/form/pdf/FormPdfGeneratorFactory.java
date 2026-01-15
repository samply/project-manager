package de.samply.form.pdf;

import de.samply.app.ProjectManagerConst;
import de.samply.pdf.PdfGenerator;
import de.samply.utils.DirectoryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;

@Component
public class FormPdfGeneratorFactory {

    private final FormPdfConverter formPdfConverter;
    private final TemplateEngine templateEngine;

    public FormPdfGeneratorFactory(FormPdfConverter formPdfConverter,
                                   @Value(ProjectManagerConst.FORM_RESOURCES_DIRECTORY_SV) String externalTemplateDirectory) throws FileNotFoundException {
        this.formPdfConverter = formPdfConverter;
        this.templateEngine = createTemplateEngine(
                DirectoryUtils.fetchExternalTemplateDirectory(externalTemplateDirectory));
    }

    public PdfGenerator createPdfGenerator() {
        return new PdfGenerator(templateEngine, formPdfConverter);
    }

    private TemplateEngine createTemplateEngine(String externalTemplateDirectory) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(createFileTemplateResolver(externalTemplateDirectory));
        return templateEngine;
    }

    private FileTemplateResolver createFileTemplateResolver(String externalTemplateDirectory) {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix(externalTemplateDirectory);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        return resolver;
    }


}
