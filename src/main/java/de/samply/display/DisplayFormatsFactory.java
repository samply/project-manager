package de.samply.display;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class DisplayFormatsFactory {

    @Bean
    public DisplayFormats displayFormats(
            @Value(ProjectManagerConst.DISPLAY_FORMATS_CONFIG_PATH_SV) Path configuredPath
    ) {
        if (configuredPath.toString().isBlank()) {
            throw new IllegalStateException(
                    ProjectManagerConst.DISPLAY_FORMATS_CONFIG_PATH + " must be configured");
        }

        try {
            return new ObjectMapper().readValue(configuredPath.toFile(), DisplayFormats.class);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to load display formats from " + configuredPath, exception);
        }
    }

}
