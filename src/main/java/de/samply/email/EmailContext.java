package de.samply.email;

import de.samply.app.ProjectManagerConst;
import de.samply.utils.Base64Utils;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ConfigurationProperties(prefix = ProjectManagerConst.EMAIL_CONTEXT_PREFIX)
@Data
public class EmailContext {

    private Map<String, String> context = new HashMap<>();


    @PostConstruct
    public void init() {
        log.info("Email Context initialized:");
        Map<String, String> tempContext = Map.copyOf(context);
        context.entrySet().stream().forEach(keyValue -> {
            log.info("\t-\t{}: {}", keyValue.getKey(), keyValue.getValue());
            Base64Utils.decodeIfNecessary(keyValue.getValue()).ifPresent(value -> tempContext.put(keyValue.getKey(), value));
        });
        context = tempContext;
    }


}
