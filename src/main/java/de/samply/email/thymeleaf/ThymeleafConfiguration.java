package de.samply.email.thymeleaf;

import de.samply.app.ProjectManagerConst;
import de.samply.utils.DirectoryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;

@Configuration
public class ThymeleafConfiguration {

    private final String externalTemplateDirectory;

    public ThymeleafConfiguration(
            @Value(ProjectManagerConst.EMAIL_TEMPLATES_DIRECTORY_SV) String externalTemplateDirectory) throws FileNotFoundException {
        this.externalTemplateDirectory = DirectoryUtils.fetchExternalTemplateDirectory(externalTemplateDirectory);
    }

    @Bean
    public FileTemplateResolver externalTemplateResolver() {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix(externalTemplateDirectory);
        resolver.setSuffix(".html");
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
