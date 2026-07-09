package de.samply.resolvers;

import de.samply.annotations.RequestParameter;
import de.samply.annotations.RequestVariable;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.ParamMetaUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class resolves method arguments annotated with either {@link RequestVariable} or {@link RequestParameter}.
 *
 * <p>
 * It handles the extraction of parameter values from HTTP request parameters (query or form parameters)
 * and, for {@link RequestVariable}, optionally from the request body (JSON).
 * </p>
 *
 * <p>
 * For {@link RequestVariable}, the resolver supports the 'required' and 'notEmpty' constraints:
 * <ul>
 *     <li>If a required parameter is missing, a {@link ServletRequestBindingException} is thrown.</li>
 *     <li>If a parameter marked 'notEmpty' is empty, a {@link ServletRequestBindingException} is thrown.</li>
 * </ul>
 * For {@link RequestParameter}, only query/form parameters are considered; the JSON body is ignored.
 * </p>
 *
 * <p>
 * After extraction, the value is converted to the target method argument type using
 * {@link org.springframework.core.convert.ConversionService} and, if necessary, Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper}.
 * </p>
 *
 * <p>
 * Additionally, resolved and raw parameter values are stored in the request-scoped
 * {@link AnnotatedParametersWrapper}, which can be used by other components (e.g., converters)
 * to access previously resolved objects or their raw values.
 * </p>
 *
 * <p>Usage examples:</p>
 *
 * <pre>
 * // Extracted from query parameters or JSON body
 * &#64;RequestVariable(name = "username", required = true, notEmpty = true)
 * String username;
 *
 * // Extracted only from query/form parameters
 * &#64;RequestParameter(name = "page")
 * Integer page;
 * </pre>
 *
 * <b>Important:</b> For {@link RequestVariable}, the value will be extracted from the JSON body
 * if not present as a request parameter. For {@link RequestParameter}, the JSON body is ignored.
 */
@Component
public class RequestVariableAndParameterMethodArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * Lazily inject the global ConversionService to avoid a circular dependency.
     * <p>
     * Context:
     * When this resolver is a @Component and we try to inject ConversionService eagerly,
     * Spring attempts to instantiate beans in the following cycle:
     * <p>
     * projectManagerWebConfig -> requestVariableMethodArgumentResolver ->
     * WebMvcAutoConfiguration.EnableWebMvcConfiguration -> ConversionService
     * <p>
     * This creates a circular reference and prevents application startup. By marking
     * ConversionService as @Lazy, Spring delays its injection until the first time it is
     * actually necessary during argument resolution, breaking the cycle.
     * <p>
     * Functionally, this has no effect on the resolver itself: argument resolution only
     * happens at request time, long after all beans are initialized.
     */
    private final ConversionService conversionService;
    private final RequestBodyCache requestBodyCache; // Injecting request-scoped bean
    private final AnnotatedParametersWrapper annotatedParametersWrapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequestVariableAndParameterMethodArgumentResolver(
            @Lazy ConversionService conversionService,
            RequestBodyCache requestBodyCache,
            AnnotatedParametersWrapper annotatedParametersWrapper) {
        this.conversionService = conversionService;
        this.requestBodyCache = requestBodyCache;
        this.annotatedParametersWrapper = annotatedParametersWrapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestVariable.class)
                || parameter.hasParameterAnnotation(RequestParameter.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {

        annotatedParametersWrapper.initializeIfNeeded(parameter, webRequest);

        ParamMeta paramMeta = ParamMetaUtils.extractParamMeta(parameter);
        assert paramMeta != null;

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        assert request != null;

        Object value = request.getParameter(paramMeta.name());

        // Only for @RequestVariable, try the JSON body
        if (value == null && paramMeta.bodyLookup()) {
            value = extractFromJsonBody(request, paramMeta.name());
        }

        // Apply defaultValue if still null
        if (value == null && !ProjectManagerConst.NOT_SET.equals(paramMeta.defaultValue())) {
            value = paramMeta.defaultValue();
        }

        // Required / notEmpty checks
        if (paramMeta.required() && value == null) {
            throw new ServletRequestBindingException("Missing required parameter: " + paramMeta.name());
        }
        if (paramMeta.notEmpty() && value instanceof String s && !StringUtils.hasText(s)) {
            throw new ServletRequestBindingException("Parameter '" + paramMeta.name() + "' must not be empty.");
        }

        // Convert and cache
        Object result = convertValue(value, parameter);
        annotatedParametersWrapper.putResolved(parameter, result);

        return result;
    }

    private Object extractFromJsonBody(HttpServletRequest request, String key) throws IOException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            return null;
        }
        Map<String, Object> jsonBody = requestBodyCache.getJsonBody(request); // Retrieve from the cache
        return (jsonBody != null) ? jsonBody.get(key) : null;
    }

    private Object convertValue(Object value, MethodParameter parameter) {
        if (value == null) return null;

        Class<?> targetType = parameter.getParameterType();

        if (value instanceof String s) {
            String trimmed = s.trim();

            // --- Case 1: target is a List ---
            if (List.class.isAssignableFrom(targetType)) {
                JavaType javaType = objectMapper.getTypeFactory()
                        .constructType(parameter.getGenericParameterType());

                if (trimmed.startsWith("[")) {
                    // Proper JSON array, e.g. [{"title":"a"},{"title":"b"}]

                    return objectMapper.readValue(trimmed, javaType);
                }

                // Comma-separated scalar list, e.g., param=val1,val2
                Class<?> elementType = getListElementType(parameter);
                return Arrays.stream(trimmed.split(","))
                        .map(String::trim)
                        .map(v -> conversionService.convert(v, elementType))
                        .toList();
            }

            // --- Case 2: Spring's ConversionService can handle it directly ---
            if (conversionService.canConvert(String.class, targetType)) {
                return conversionService.convert(s, targetType);
            }

            // --- Case 3: fallback to Jackson ---
            boolean looksLikeJson = trimmed.startsWith("{") || trimmed.startsWith("[");
            if (looksLikeJson) {
                return objectMapper.readValue(trimmed, targetType); // parse JSON text (e.g., FormField)
            } else {
                return objectMapper.convertValue(s, targetType); // scalar coercion (enums, etc.)
            }
        }

        // --- value is not a String, e.g., already a Map/LinkedHashMap from elsewhere ---
        return objectMapper.convertValue(value, targetType);
    }

    private Class<?> getListElementType(MethodParameter parameter) {
        ParameterizedType type = (ParameterizedType) parameter.getGenericParameterType();
        return (Class<?>) type.getActualTypeArguments()[0];
    }

}