package de.samply.frontend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.Base64Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ActionExplanationsFactory {

    @Bean
    public ActionExplanations actionExplanations(@Value(ProjectManagerConst.ACTION_EXPLANATION_CONFIG_SV) String template) throws JsonProcessingException {
        Optional<String> decodedTemplates = Base64Utils.decodeIfNecessary(template);
        if (decodedTemplates.isEmpty()) {
            throw new RuntimeException("No action explanation config found");
        }
        return new ObjectMapper().readValue(decodedTemplates.get(), ActionExplanations.class);
    }

}
