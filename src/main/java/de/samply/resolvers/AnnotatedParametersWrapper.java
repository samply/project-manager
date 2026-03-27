package de.samply.resolvers;

import de.samply.annotations.RequestVariable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AnnotatedParametersWrapper {

    private final Map<Class<? extends Annotation>, Object> resolved = new HashMap<>();
    private final Map<Class<? extends Annotation>, Object> rawValues = new HashMap<>();

    private final RequestBodyCache requestBodyCache;

    private boolean initialized = false;

    public AnnotatedParametersWrapper(RequestBodyCache requestBodyCache) {
        this.requestBodyCache = requestBodyCache;
    }

    // =========================================================
    // 🔹 INITIALIZATION (called once per request)
    // =========================================================

    public void initializeIfNeeded(MethodParameter parameter,
                                   NativeWebRequest webRequest) {

        if (initialized) return;

        Method method = parameter.getMethod();
        if (method == null) return;

        MethodParameter[] parameters = IntStream
                .range(0, method.getParameterCount())
                .mapToObj(i -> new MethodParameter(method, i))
                .toArray(MethodParameter[]::new);

        initialize(parameters, webRequest);
        initialized = true;
    }

    private void initialize(MethodParameter[] parameters,
                            NativeWebRequest webRequest) {

        HttpServletRequest servletRequest =
                webRequest.getNativeRequest(HttpServletRequest.class);

        Map<String, Object> body = Optional.ofNullable(servletRequest)
                .map(req -> {
                    try {
                        return requestBodyCache.getJsonBody(req);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(Collections.emptyMap());

        Arrays.stream(parameters)
                .forEach(param -> processParameter(param, webRequest, body));
    }

    private void processParameter(MethodParameter param,
                                  NativeWebRequest webRequest,
                                  Map<String, Object> body) {

        String name = extractRequestVariableName(param);
        if (name == null) return;

        Object value = Optional.ofNullable(webRequest.getParameter(name))
                .orElseGet(() -> (String) body.get(name));

        if (value == null) return;

        extractRelevantAnnotations(param)
                .forEach(annotation -> rawValues.put(annotation, value));
    }

    // =========================================================
    // 🔹 RESOLVED VALUES
    // =========================================================

    public <T> void putResolved(MethodParameter parameter, T value) {
        if (value == null) return;

        extractRelevantAnnotations(parameter)
                .forEach(annotation -> resolved.put(annotation, value));
    }

    public <T> Optional<T> getResolved(Class<? extends Annotation> annotation, Class<T> type) {
        Object value = resolved.get(annotation);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    // =========================================================
    // 🔹 RAW VALUES
    // =========================================================

    public <T> Optional<T> getRaw(Class<? extends Annotation> annotation, Class<T> type) {
        Object value = rawValues.get(annotation);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    // =========================================================
    // 🔹 INTERNAL HELPERS
    // =========================================================

    private List<Class<? extends Annotation>> extractRelevantAnnotations(MethodParameter parameter) {

        return Arrays.stream(parameter.getParameterAnnotations())
                .map(a -> (Class<? extends Annotation>) a.annotationType())
                .filter(annotation -> !annotation.equals(RequestVariable.class))
                .collect(Collectors.toList());
    }

    private String extractRequestVariableName(MethodParameter parameter) {

        return Optional.ofNullable(parameter.getParameterAnnotation(RequestVariable.class))
                .map(RequestVariable::name)
                .orElse(null);
    }

}