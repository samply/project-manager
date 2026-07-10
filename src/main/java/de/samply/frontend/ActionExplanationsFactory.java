package de.samply.frontend;

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
public class ActionExplanationsFactory {

    @Bean
    public ActionExplanations actionExplanations(@Value(ProjectManagerConst.ACTION_EXPLANATION_CONFIG_PATH_SV) Path templatesPath) {
        try {
            return new ObjectMapper().readValue(templatesPath.toFile(), ActionExplanations.class);
        } catch (IOException e) {
            log.error("Action explanations file not found {}", templatesPath);
            throw new RuntimeException(e);
        }
    }

}
