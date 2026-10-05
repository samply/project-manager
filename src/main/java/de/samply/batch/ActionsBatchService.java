package de.samply.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.samply.annotations.FrontendAction;
import de.samply.app.ProjectManagerConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Runs several read actions of the frontend in one request (see plans/2026-09-30-plan-batch-getter-endpoint.md).
 * <p>
 * Every entry names a frontend action of a GET endpoint and its parameters. The endpoint's controller method is called
 * in this process, through the same argument resolvers and aspects as for its own request, so the constraints of the
 * endpoint (roles, states, project) apply unchanged: an entry that is refused is answered with the refusal of the
 * aspect. Nothing about the constraints is checked here.
 * <p>
 * The rule of the security filter chain for the endpoint's URL (SecurityConfiguration.addAuthorityMapping) does not
 * run for an entry. It asks for the organisation roles of the endpoint's RoleConstraints, which the role constraints
 * aspect checks as well, from the same mapping of the user's groups. The only difference is the status: the filter
 * chain answers 403, the aspect 405.
 * <p>
 * Entries run in parallel on the threads of the actions batch executor. One failing entry does not affect the others.
 */
@Slf4j
@Service
public class ActionsBatchService {

    private final ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider;
    private final ObjectProvider<RequestMappingHandlerAdapter> handlerAdapterProvider;
    private final ThreadPoolTaskExecutor executor;
    private final int maxEntries;

    // Same output settings as the controller, which serializes the answers of the endpoints
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    // Built on first use: the handler mapping and adapter are not complete while this service is created
    private volatile Invoker invoker;

    public ActionsBatchService(
            @Qualifier("requestMappingHandlerMapping") ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider,
            @Qualifier("requestMappingHandlerAdapter") ObjectProvider<RequestMappingHandlerAdapter> handlerAdapterProvider,
            @Qualifier(ProjectManagerConst.ASYNC_ACTIONS_BATCH_EXECUTOR) ThreadPoolTaskExecutor executor,
            @Value(ProjectManagerConst.ACTIONS_BATCH_MAX_ENTRIES_SV) int maxEntries) {
        this.handlerMappingProvider = handlerMappingProvider;
        this.handlerAdapterProvider = handlerAdapterProvider;
        this.executor = executor;
        this.maxEntries = maxEntries;
    }

    /** A batch with more entries is refused as a whole, before any entry runs (see the controller). */
    public int getMaxEntries() {
        return maxEntries;
    }

    /**
     * @param requests the entries of the batch, each under an id chosen by the caller
     * @return the result of each entry under its id, in the order of the requests
     */
    public Map<String, ActionsBatchResult> fetchActionsBatch(Map<String, ActionsBatchRequest> requests) {
        // Everything an entry needs from the batch request is read here, on the thread of the batch request
        HttpServletRequest batchRequest =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        HttpSession session = batchRequest.getSession();
        SecurityContext securityContext = SecurityContextHolder.getContext();
        // The projects and bridgeheads the entries resolve, loaded once for the whole batch
        ActionsBatchLookups lookups = new ActionsBatchLookups();

        // runEntry answers its own failures; exceptionally only covers an entry that could not be run at all
        Map<String, CompletableFuture<ActionsBatchResult>> futures = new LinkedHashMap<>();
        requests.forEach((id, entry) -> futures.put(id, executor
                .submitCompletable(() -> runEntry(entry, batchRequest, session, lookups, securityContext))
                .exceptionally(e -> {
                    log.warn("Entry '{}' of an actions batch failed", id, e);
                    return toErrorResult(e.getCause() != null ? e.getCause() : e, System.nanoTime());
                })));

        // The batch waits for every entry: the entries use the batch request, which must not end before them.
        // join, unlike get, keeps waiting when the thread is interrupted.
        Map<String, ActionsBatchResult> results = new LinkedHashMap<>();
        futures.forEach((id, future) -> results.put(id, future.join()));
        return results;
    }

    private ActionsBatchResult runEntry(ActionsBatchRequest entry, HttpServletRequest batchRequest, HttpSession session,
                                        ActionsBatchLookups lookups, SecurityContext securityContext) {
        long start = System.nanoTime();
        try {
            if (entry == null || entry.action() == null) {
                return error(HttpStatus.BAD_REQUEST, "An entry needs an action", start);
            }
            String action = entry.action();
            Invoker invoker = invoker();
            HandlerMethod handlerMethod = invoker.readActions.get(action);
            if (handlerMethod == null) {
                return invoker.otherActions.contains(action)
                        ? error(HttpStatus.METHOD_NOT_ALLOWED, "Action " + action + " is not a read action (GET)", start)
                        : error(HttpStatus.NOT_FOUND, "Unknown action " + action, start);
            }
            Map<String, String> parameters = toParameters(entry.params());
            Object answer = invoke(invoker, handlerMethod,
                    new ActionsBatchEntryRequest(batchRequest, session, parameters, lookups), securityContext);
            return toResult(action, answer, start);
        } catch (Throwable e) {
            return toErrorResult(e, start);
        }
    }

    /**
     * Calls the controller method like Spring MVC does for a request. The request attributes and the security context
     * are bound to the thread for the call: request- and session-scoped beans and the aspects read them from there.
     */
    private Object invoke(Invoker invoker, HandlerMethod handlerMethod, ActionsBatchEntryRequest entryRequest,
                          SecurityContext securityContext) throws Exception {
        RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
        SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
        ServletRequestAttributes attributes = new ServletRequestAttributes(entryRequest);
        RequestContextHolder.setRequestAttributes(attributes);
        SecurityContextHolder.setContext(securityContext);
        try {
            InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod.createWithResolvedBean());
            invocable.setHandlerMethodArgumentResolvers(invoker.argumentResolvers);
            invocable.setDataBinderFactory(invoker.dataBinderFactory);
            invocable.setParameterNameDiscoverer(new DefaultParameterNameDiscoverer());
            return invocable.invokeForRequest(new ServletWebRequest(entryRequest), new ModelAndViewContainer());
        } finally {
            try {
                attributes.requestCompleted();
            } finally {
                // Also when a destruction callback of a request-scoped bean fails. An entry can also run on the thread
                // of the batch request (executor saturated): leave it as it was
                if (previousAttributes != null) {
                    RequestContextHolder.setRequestAttributes(previousAttributes);
                    SecurityContextHolder.setContext(previousSecurityContext);
                } else {
                    RequestContextHolder.resetRequestAttributes();
                    SecurityContextHolder.clearContext();
                }
            }
        }
    }

    // Parameters as the endpoint would get them in its query string
    private Map<String, String> toParameters(Map<String, Object> params) {
        Map<String, String> parameters = new LinkedHashMap<>();
        Optional.ofNullable(params).orElse(Map.of()).forEach((name, value) -> {
            // Text, numbers and booleans as they are; lists and objects as JSON
            JsonNode node = objectMapper.valueToTree(value);
            if (!node.isNull()) parameters.put(name, node.isValueNode() ? node.asText() : node.toString());
        });
        return parameters;
    }

    private ActionsBatchResult toResult(String action, Object answer, long start) throws JsonProcessingException {
        int status = HttpStatus.OK.value();
        Object body = answer;
        MediaType contentType = null;
        if (answer instanceof ResponseEntity<?> responseEntity) {
            status = responseEntity.getStatusCode().value();
            body = responseEntity.getBody();
            contentType = responseEntity.getHeaders().getContentType();
        }
        if (status >= 400) {
            return toErrorResult(status, body, start);
        }
        if (isBinary(body, contentType)) {
            return error(HttpStatus.NOT_ACCEPTABLE,
                    "The answer of action " + action + " is not JSON: call its endpoint instead", start);
        }
        return ActionsBatchResult.success(toJson(body), durationMs(start));
    }

    private boolean isBinary(Object body, MediaType contentType) {
        if (body instanceof Resource || body instanceof byte[] || body instanceof InputStream) {
            return true;
        }
        return body != null && contentType != null
                && !contentType.isCompatibleWith(MediaType.APPLICATION_JSON)
                && !contentType.isCompatibleWith(MediaType.TEXT_PLAIN);
    }

    // The endpoints answer with JSON as text. Other text is passed on as a JSON string, as the browser would read it.
    private JsonNode toJson(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof String text) {
            if (text.isBlank()) {
                return null;
            }
            try {
                return objectMapper.readTree(text);
            } catch (JsonProcessingException e) {
                return TextNode.valueOf(text);
            }
        }
        return objectMapper.valueToTree(body);
    }

    private ActionsBatchResult toErrorResult(int status, Object body, long start) {
        String message = switch (body) {
            case String text when !text.isBlank() -> text;
            case ByteArrayResource resource -> new String(resource.getByteArray(), StandardCharsets.UTF_8);
            case null, default -> null;
        };
        // An endpoint answers an exception with its stack trace as body
        if (status == HttpStatus.INTERNAL_SERVER_ERROR.value() && message != null) {
            return ActionsBatchResult.error(status, firstLine(message), message, durationMs(start));
        }
        return ActionsBatchResult.error(status, message, null, durationMs(start));
    }

    // Exceptions thrown before the controller method answers, e.g. while resolving its arguments
    private ActionsBatchResult toErrorResult(Throwable e, long start) {
        int status = switch (e) {
            case ErrorResponse errorResponse -> errorResponse.getStatusCode().value();
            case TypeMismatchException _ -> HttpStatus.BAD_REQUEST.value();
            default -> HttpStatus.INTERNAL_SERVER_ERROR.value();
        };
        String stacktrace = ExceptionUtils.getStackTrace(e);
        return ActionsBatchResult.error(status, firstLine(stacktrace),
                status == HttpStatus.INTERNAL_SERVER_ERROR.value() ? stacktrace : null, durationMs(start));
    }

    private ActionsBatchResult error(HttpStatus status, String message, long start) {
        return ActionsBatchResult.error(status.value(), message, null, durationMs(start));
    }

    private String firstLine(String text) {
        return text.lines().findFirst().map(String::trim).orElse(null);
    }

    private long durationMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private Invoker invoker() {
        Invoker result = invoker;
        if (result == null) {
            synchronized (this) {
                result = invoker;
                if (result == null) {
                    invoker = result = createInvoker();
                }
            }
        }
        return result;
    }

    private Invoker createInvoker() {
        RequestMappingHandlerAdapter handlerAdapter = handlerAdapterProvider.getObject();
        Map<String, HandlerMethod> readActions = new HashMap<>();
        Set<String> otherActions = new HashSet<>();
        handlerMappingProvider.getObject().getHandlerMethods().forEach((mapping, handlerMethod) -> {
            FrontendAction frontendAction = handlerMethod.getMethodAnnotation(FrontendAction.class);
            if (frontendAction != null) {
                if (mapping.getMethodsCondition().getMethods().contains(RequestMethod.GET)) {
                    readActions.put(frontendAction.action(), handlerMethod);
                } else {
                    otherActions.add(frontendAction.action());
                }
            }
        });
        HandlerMethodArgumentResolverComposite argumentResolvers = new HandlerMethodArgumentResolverComposite();
        argumentResolvers.addResolvers(handlerAdapter.getArgumentResolvers());
        return new Invoker(Map.copyOf(readActions), Set.copyOf(otherActions), argumentResolvers,
                new ServletRequestDataBinderFactory(null, handlerAdapter.getWebBindingInitializer()));
    }

    /**
     * @param readActions  frontend actions of GET endpoints: the only ones a batch may call
     * @param otherActions frontend actions of the other endpoints, to tell them apart from unknown actions
     */
    private record Invoker(Map<String, HandlerMethod> readActions,
                           Set<String> otherActions,
                           HandlerMethodArgumentResolverComposite argumentResolvers,
                           ServletRequestDataBinderFactory dataBinderFactory) {
    }

}
