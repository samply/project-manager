package de.samply.email;

import de.samply.app.ProjectManagerConst;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ConfigurationProperties(prefix = ProjectManagerConst.EMAIL_CONTEXT)
@Data
public class EmailContext {

    private Map<String, String> context = new HashMap<>();


    @PostConstruct
    public void init() {
        log.info("Email Context initialized:");
        context.entrySet().stream().forEach(keyValue -> log.info("\t-\t{}: {}", keyValue.getKey(), keyValue.getValue()));
    }

}
