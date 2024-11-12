package de.samply.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.Base64Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmailTemplatesFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public EmailTemplates createEmailTemplates(@Value(ProjectManagerConst.EMAIL_TEMPLATES_CONFIG_SV) String templates) throws JsonProcessingException {
        Optional<String> decodedTemplates = Base64Utils.decodeIfNecessary(templates);
        if (decodedTemplates.isEmpty()) {
            throw new RuntimeException("No template found");
        }
        return objectMapper.readValue(decodedTemplates.get(), EmailTemplates.class);
    }

}
