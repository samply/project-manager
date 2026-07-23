package de.samply.form.pdf;

import de.samply.app.ProjectManagerConst;
import de.samply.pdf.PdfGenerator;
import de.samply.utils.FileExtension;
import de.samply.utils.directory.ExistingDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Component
public class FormPdfGeneratorFactory {

    private final FormPdfConverter formPdfConverter;
    private final TemplateEngine templateEngine;

    public FormPdfGeneratorFactory(FormPdfConverter formPdfConverter,
                                   @Value(ProjectManagerConst.FORM_RESOURCES_DIRECTORY_SV) ExistingDirectory externalTemplateDirectory) {
        this.formPdfConverter = formPdfConverter;
        this.templateEngine = createTemplateEngine(externalTemplateDirectory.path().toAbsolutePath());
    }

    public PdfGenerator createPdfGenerator() {
        return new PdfGenerator(templateEngine, formPdfConverter);
    }

    private TemplateEngine createTemplateEngine(Path externalTemplateDirectory) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(createFileTemplateResolver(externalTemplateDirectory));
        return templateEngine;
    }

    private FileTemplateResolver createFileTemplateResolver(Path externalTemplateDirectory) {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix(externalTemplateDirectory + File.separator);
        resolver.setSuffix("." + FileExtension.HTML.value());
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        return resolver;
    }


}
