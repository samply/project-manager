package de.samply.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Component
public class EmailTemplatesFactory {


    @Bean
    public EmailTemplates createEmailTemplates(@Value(ProjectManagerConst.EMAIL_TEMPLATES_CONFIG_PATH_SV) Path templatesPath) {
        try {
            return new ObjectMapper().readValue(templatesPath.toFile(), EmailTemplates.class);
        } catch (IOException e) {
            log.error("Configuration file for email templates not found: {}", templatesPath);
            throw new RuntimeException(e);
        }
    }

}
