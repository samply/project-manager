package de.samply.resolvers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestBodyCache {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Object> jsonBody;

    public Map<String, Object> getJsonBody(HttpServletRequest request) throws IOException {
        if (jsonBody == null) {
            try {
                jsonBody = objectMapper.readValue(
                        request.getInputStream(),
                        new TypeReference<>() {
                        }
                );
            } catch (com.fasterxml.jackson.databind.exc.MismatchedInputException e) {
                jsonBody = Collections.emptyMap();
            }
        }
        return jsonBody;
    }

}
