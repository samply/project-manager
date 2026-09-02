package de.samply.cache;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.EnumMap;

import static org.assertj.core.api.Assertions.assertThat;

class CachePolicyFilterTest {

    @Test
    void appliesFallbackPolicyWhenResponseHasNoCacheHeader() throws Exception {
        CacheConfiguration configuration = new CacheConfiguration(300, 86400);
        EnumMap<CacheResource, CachePolicy> policies = new EnumMap<>(CacheResource.class);
        policies.put(CacheResource.AUTHENTICATED_GET, CachePolicy.NO_STORE);
        configuration.setConfig(policies);
        CachePolicyFilter filter = new CachePolicyFilter(configuration);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/project"), response,
                (request, result) -> { });

        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void preservesHeaderSetBySpecializedHandler() throws Exception {
        CacheConfiguration configuration = new CacheConfiguration(300, 86400);
        CachePolicyFilter filter = new CachePolicyFilter(configuration);
        FilterChain specializedHandler = (request, response) ->
                ((MockHttpServletResponse) response).setHeader("Cache-Control", "public, max-age=3600");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/assets/logo.svg"), response,
                specializedHandler);

        assertThat(response.getHeader("Cache-Control")).isEqualTo("public, max-age=3600");
    }

    @Test
    void usesCategoryDeclaredByEndpoint() throws Exception {
        CacheConfiguration configuration = new CacheConfiguration(300, 86400);
        EnumMap<CacheResource, CachePolicy> policies = new EnumMap<>(CacheResource.class);
        policies.put(CacheResource.PUBLIC_INFORMATION, CachePolicy.LONG);
        configuration.setConfig(policies);
        CachePolicyFilter filter = new CachePolicyFilter(configuration);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/info");
        request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,
                new HandlerMethod(new CategorizedHandler(),
                        CategorizedHandler.class.getDeclaredMethod("handle")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getHeader("Cache-Control")).isEqualTo("max-age=86400, public");
    }

    private static class CategorizedHandler {
        @CacheCategory(CacheResource.PUBLIC_INFORMATION)
        public void handle() {
        }
    }
}
