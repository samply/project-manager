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
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@ConfigurationProperties(prefix = ProjectManagerConst.EMAIL_CONTEXT_PREFIX)
@Data
public class EmailContext {

    private Map<String, String> context = new HashMap<>();


    // Keys may contain the "-" character, which is represented as "MINUS" in environment variables.
    // If a key is Base64-encoded, it should start with the prefix "B64" to indicate its encoding.
    @PostConstruct
    public void init() {
        log.info("Email Context initialized: ({})", context.entrySet().size());
        Map<String, String> tempContext = new HashMap<>();
        context.entrySet().stream().forEach(keyValue -> {
            String key = replaceHyphen(keyValue.getKey());
            AtomicReference<String> value = new AtomicReference<>(keyValue.getValue());
            if (key.startsWith(ProjectManagerConst.BASE_64)) {
                key = key.replaceFirst(ProjectManagerConst.BASE_64, "");
                Base64Utils.decodeIfNecessary(keyValue.getValue()).ifPresent(value::set);
            }
            log.info("\t-\t{}: {}", key, value.get());
            tempContext.put(key, value.get());
        });
        context = tempContext;
    }

    private String replaceHyphen(String var) {
        return var.replace(ProjectManagerConst.HYPHEN, "-");
    }


}
