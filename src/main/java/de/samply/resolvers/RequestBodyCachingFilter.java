package de.samply.resolvers;

import de.samply.app.ProjectManagerConst;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestBodyCachingFilter extends OncePerRequestFilter {

    private final int cacheLimit;

    public RequestBodyCachingFilter(
            @Value(ProjectManagerConst.REQUEST_CACHE_LIMIT_SV) int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, cacheLimit);
        filterChain.doFilter(wrapped, response);
    }
}