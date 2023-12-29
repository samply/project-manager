package de.samply.email;

import de.samply.app.ProjectManagerConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class ThymeleafConfiguration {

    private final String externalTemplateDirectory;

    public ThymeleafConfiguration(
            @Value(ProjectManagerConst.EMAIL_TEMPLATES_DIRECTORY_SV) String externalTemplateDirectory) throws FileNotFoundException {
        this.externalTemplateDirectory = fetchExternalTemplateDirectory(externalTemplateDirectory);
    }

    private String fetchExternalTemplateDirectory(String environmentVariable) throws FileNotFoundException {
        if (StringUtils.hasText(environmentVariable) && Files.isDirectory(Path.of(environmentVariable))) {
            return (environmentVariable.endsWith("/") || environmentVariable.endsWith("\\")) ?
                    environmentVariable : environmentVariable + "/";
        } else {
            throw new FileNotFoundException("Email Templates Directory not set or set to incorrect directory: " + environmentVariable);
        }
    }

    @Bean
    public FileTemplateResolver externalTemplateResolver() {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix(externalTemplateDirectory);
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        return resolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(externalTemplateResolver());
        return engine;
    }

}
