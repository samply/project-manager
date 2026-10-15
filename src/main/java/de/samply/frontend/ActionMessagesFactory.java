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
public class ActionMessagesFactory {

    @Bean
    public ActionMessages actionMessages(@Value(ProjectManagerConst.ACTION_MESSAGES_CONFIG_PATH_SV) Path messagesPath) {
        try {
            return new ObjectMapper().readValue(messagesPath.toFile(), ActionMessages.class);
        } catch (IOException e) {
            log.error("Action messages file not found {}", messagesPath);
            throw new RuntimeException(e);
        }
    }
}
