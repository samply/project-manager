package de.samply.resolvers;

import de.samply.annotations.RequestParameter;
import de.samply.annotations.RequestVariable;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.ParamMetaUtils;
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
import java.util.concurrent.atomic.AtomicReference;
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

    public void initializeIfNeeded(MethodParameter parameter, NativeWebRequest webRequest) {
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

    private void initialize(MethodParameter[] parameters, NativeWebRequest webRequest) {
        var request = webRequest.getNativeRequest(HttpServletRequest.class);
        var bodyRef = new AtomicReference<Map<String, Object>>();

        Arrays.stream(parameters)
                .map(ParamMetaUtils::extractParamMeta)
                .filter(Objects::nonNull)
                .forEach(meta -> {
                    // Lazy-load body only if needed
                    if (meta.bodyLookup() && bodyRef.get() == null) {
                        var body = Optional.ofNullable(request)
                                .map(req -> {
                                    try { return requestBodyCache.getJsonBody(req); }
                                    catch (IOException e) { throw new RuntimeException(e); }
                                })
                                .orElse(Collections.emptyMap());
                        bodyRef.set(body);
                    }

                    processParameter(meta, webRequest, bodyRef.get());
                });
    }

    private void processParameter(ParamMeta meta, NativeWebRequest webRequest, Map<String, Object> body) {
        var initialValue = (Object) webRequest.getParameter(meta.name());
        if (initialValue == null && meta.bodyLookup()) {
            initialValue = body.get(meta.name());
        }

        var finalValue = initialValue != null ? initialValue
                : (!ProjectManagerConst.NOT_SET.equals(meta.defaultValue()) ? meta.defaultValue() : null);

        if (finalValue == null) return;

        extractRelevantAnnotations(meta.parameter())
                .forEach(annotation -> rawValues.put(annotation, finalValue));
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
                .filter(annotation -> !annotation.equals(RequestVariable.class) && !annotation.equals(RequestParameter.class))
                .collect(Collectors.toList());
    }

}