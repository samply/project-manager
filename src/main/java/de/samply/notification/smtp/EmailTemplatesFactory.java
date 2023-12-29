package de.samply.notification.smtp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.Base64Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplatesFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public EmailTemplates createEmailTemplates(@Value(ProjectManagerConst.EMAIL_TEMPLATES_CONFIG_SV) String templates) throws JsonProcessingException {
        return objectMapper.readValue(Base64Utils.decodeIfNecessary(templates), EmailTemplates.class);
    }

}
