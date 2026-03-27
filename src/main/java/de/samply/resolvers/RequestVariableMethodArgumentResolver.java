package de.samply.resolvers;

import de.samply.annotations.RequestVariable;
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
import java.util.Map;

/**
 * This class resolves method arguments annotated with {@link RequestVariable}.
 * It handles the extraction of parameter values either from HTTP request parameters
 * or the request body (JSON). It supports constraints such as 'required' and 'notEmpty'
 * for parameters and performs type conversion of the extracted values into the appropriate method argument type.
 * <p>
 * It first attempts to extract the parameter value from query parameters (via `@RequestParam`).
 * If the value is not found, it will then attempt to extract it from the JSON body of the request.
 * If both methods fail to find the value and the parameter is marked as required, an exception will be thrown.
 * <p>
 * It also ensures that parameters marked as 'notEmpty' contain a non-empty value. If a parameter is empty
 * when it should not be, a {@link ServletRequestBindingException} will be thrown.
 * <p>
 * The parameter value is then converted to the correct type using a {@link org.springframework.core.convert.ConversionService}.
 *
 * <p>Usage example:</p>
 *
 * <pre>
 * &#64;RequestVariable(name = "username", required = true, notEmpty = true)
 * String username;
 * </pre>
 *
 * <b>Important:</b> The value will be extracted either from request parameters (e.g., `username=JohnDoe`)
 * or the JSON body (e.g., `{"username": "JohnDoe"}`), depending on where the client provides it.
 */
@Component
public class RequestVariableMethodArgumentResolver implements HandlerMethodArgumentResolver {

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

    public RequestVariableMethodArgumentResolver(
            @Lazy ConversionService conversionService,
            RequestBodyCache requestBodyCache,
            AnnotatedParametersWrapper annotatedParametersWrapper) {
        this.conversionService = conversionService;
        this.requestBodyCache = requestBodyCache;
        this.annotatedParametersWrapper = annotatedParametersWrapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestVariable.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {

        annotatedParametersWrapper.initializeIfNeeded(parameter, webRequest);

        RequestVariable requestVariable = parameter.getParameterAnnotation(RequestVariable.class);
        assert requestVariable != null;
        String paramName = requestVariable.name();
        boolean required = requestVariable.required();
        boolean notEmpty = requestVariable.notEmpty();

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        assert request != null;
        Object value = request.getParameter(paramName);

        // Try to get value from query parameters
        if (value == null) {
            // Try to get value from the JSON body
            value = extractFromJsonBody(request, paramName);
        }

        // Handle required constraint
        if (required && value == null) {
            throw new ServletRequestBindingException("Missing required parameter: " + paramName);
        }

        // Handle notEmpty constraint
        if (notEmpty && value instanceof String && !StringUtils.hasText((String) value)) {
            throw new ServletRequestBindingException("Parameter '" + paramName + "' must not be empty.");
        }

        // Convert a value to a target type using ConversionService
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

        // 1️⃣ If the value is a String, try Spring's ConversionService first
        if (value instanceof String s) {
            if (conversionService.canConvert(String.class, targetType)) {
                return conversionService.convert(s, targetType);
            }
            // fallback to ObjectMapper if ConversionService cannot convert
            return objectMapper.convertValue(s, targetType);
        }

        // 2️⃣ For non-String values (e.g., JSON Maps, Lists, Numbers), delegate to ObjectMapper
        JavaType javaType = objectMapper.getTypeFactory()
                .constructType(parameter.getGenericParameterType());
        return objectMapper.convertValue(value, javaType);
    }

}