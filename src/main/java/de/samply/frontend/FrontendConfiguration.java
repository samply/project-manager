package de.samply.frontend;

import de.samply.app.ProjectManagerConst;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    private void postConstruct() {
        Map<String, String> tempSites = new HashMap<>();
        sites.forEach((siteId, sitePath) -> tempSites.put(replaceHyphen(siteId), sitePath));
        sites = tempSites;
    }

    private String replaceHyphen(String var) {
        return var.replace(ProjectManagerConst.HYPHEN, "-");
    }

    public Optional<String> getSitePath(String site) {
        return Optional.ofNullable(sites.get(site));
    }

}
