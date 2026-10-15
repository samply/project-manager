package de.samply.cache;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Applies the configured fallback policy to backend API responses. */
public class CachePolicyFilter extends OncePerRequestFilter {

    private final CacheConfiguration cacheConfiguration;

    public CachePolicyFilter(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        // Resource handlers and specialized controllers already provide their
        // own policy. Do not duplicate endpoint knowledge here. This also
        // covers /assets/** because its resource handler sets the header first.
        if (response.containsHeader(HttpHeaders.CACHE_CONTROL)) {
            return;
        }

        CacheResource resource = resourceFor(request);
        response.setHeader(HttpHeaders.CACHE_CONTROL,
                cacheConfiguration.cacheControl(resource).getHeaderValue());
    }

    private CacheResource resourceFor(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod handlerMethod) {
            CacheCategory category = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getMethod(), CacheCategory.class);
            if (category != null) {
                return category.value();
            }
        }
        return HttpMethod.GET.matches(request.getMethod())
                ? CacheResource.AUTHENTICATED_GET
                : CacheResource.MUTATION_RESPONSES;
    }
}
