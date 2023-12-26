package de.samply.frontend;

import de.samply.app.ProjectManagerConst;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = ProjectManagerConst.FRONTEND_CONFIG)
@Data
public class FrontendConfiguration {

    private String baseUrl;
    private Map<String, String> sites = new HashMap<>();

    public Optional<String> getSitePath(String site) {
        return Optional.ofNullable(sites.get(site));
    }

}
