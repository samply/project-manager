package de.samply.resolvers;

import de.samply.annotations.RequestVariable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
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

    private final DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
    ;
    private final RequestBodyCache requestBodyCache; // Injecting request-scoped bean
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequestVariableMethodArgumentResolver(RequestBodyCache requestBodyCache) {
        this.requestBodyCache = requestBodyCache;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestVariable.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {

        RequestVariable requestVariable = parameter.getParameterAnnotation(RequestVariable.class);
        String paramName = requestVariable.name();
        boolean required = requestVariable.required();
        boolean notEmpty = requestVariable.notEmpty();

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        Object value = request.getParameter(paramName);

        // Try to get value from query parameters
        if (value == null) {
            // Try to get value from JSON body
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

        // Convert value to target type using ConversionService
        return convertValue(value, parameter);
    }

    private Object extractFromJsonBody(HttpServletRequest request, String key) throws IOException {
        Map<String, Object> jsonBody = requestBodyCache.getJsonBody(request); // Retrieve from the cache
        return (jsonBody != null) ? jsonBody.get(key) : null;
    }

    private Object convertValue(Object value, MethodParameter parameter) {
        if (value == null) return null;

        Class<?> targetType = parameter.getParameterType();

        if (value instanceof String s) {
            return conversionService.convert(s, targetType);
        }

        JavaType javaType =
                objectMapper.getTypeFactory()
                        .constructType(parameter.getGenericParameterType());

        return objectMapper.convertValue(value, javaType);
    }


}