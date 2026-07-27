package de.samply.email.thymeleaf;

import de.samply.app.ProjectManagerConst;
import de.samply.utils.FileExtension;
import de.samply.utils.directory.ExistingDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Configuration
public class ThymeleafConfiguration {

    private final String externalTemplateDirectory;

    public ThymeleafConfiguration(
            @Value(ProjectManagerConst.EMAIL_TEMPLATES_DIRECTORY_SV) ExistingDirectory externalTemplateDirectory) {
        this.externalTemplateDirectory = externalTemplateDirectory.path().toAbsolutePath().toString();
    }

    @Bean
    public FileTemplateResolver externalTemplateResolver() {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix(externalTemplateDirectory + File.separator);
        resolver.setSuffix("." + FileExtension.HTML.value());
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        return resolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(externalTemplateResolver());
        engine.addDialect(new ProjectManagerDialect());
        return engine;
    }

}
