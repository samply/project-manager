package de.samply.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestCacheFilter extends OncePerRequestFilter {

    private RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null && !isAuthenticationRequest(request)) {
            requestCache.saveRequest(request, response);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAuthenticationRequest(HttpServletRequest request) {
        // Add conditions here to identify your authentication-related endpoints
        // For example:
        return request.getRequestURI().equals("/oauth2/authorization/oidc");
    }
}
