package de.samply.frontend;

import de.samply.app.ProjectManagerConst;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = ProjectManagerConst.FRONTEND_CONFIG)
@Data
public class FrontendConfiguration {

    private String baseUrl;
    private Map<String, String> sites = new HashMap<>();
    private Map<String, String> variables = new HashMap<>();


    @PostConstruct
    private void postConstruct() {
        Map<String, String> tempSites = new HashMap<>();
        sites.forEach((siteId, sitePath) -> tempSites.put(replaceHyphen(siteId), sitePath));
        sites = tempSites;

        Map<String, String> tempVariables = new HashMap<>();
        variables.forEach((key, value) -> tempVariables.put(normalizeVariableKey(key), value));
        variables = tempVariables;

        log.info("----------------------------------------");
        log.info("Frontend Configuration:");
        log.info("Base URL: {}", baseUrl);
        log.info("Sites:");
        sites.forEach((key, value) ->
                log.info("- {} : {}", key, value)
        );
        log.info("----------------------------------------");
    }

    private String replaceHyphen(String var) {
        return var.replace(ProjectManagerConst.HYPHEN, "-");
    }

    private String normalizeVariableKey(String key) {
        return key
                .replace(".", "_")
                .toUpperCase(Locale.ROOT);
    }

    public Optional<String> getSitePath(String site) {
        return Optional.ofNullable(sites.get(site));
    }

}
