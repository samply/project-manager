package de.samply.cache;

import de.samply.app.ProjectManagerConst;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/** Runtime cache policies bound from CACHE_POLICY_CONFIG_* environment variables. */
@Configuration
@ConfigurationProperties(prefix = ProjectManagerConst.CACHE_POLICY_PREFIX)
@Getter
public class CacheConfiguration {

    /** Maps resource categories to named policies, e.g. CACHE_POLICY_CONFIG_BACKEND_ASSETS. */
    @Setter
    private Map<CacheResource, CachePolicy> config = new EnumMap<>(CacheResource.class);

    private final long shortMaxAgeSeconds;
    private final long longMaxAgeSeconds;

    public CacheConfiguration(
            @Value(ProjectManagerConst.CACHE_POLICY_SHORT_MAX_AGE_SECONDS_SV) long shortMaxAgeSeconds,
            @Value(ProjectManagerConst.CACHE_POLICY_LONG_MAX_AGE_SECONDS_SV) long longMaxAgeSeconds) {
        this.shortMaxAgeSeconds = shortMaxAgeSeconds;
        this.longMaxAgeSeconds = longMaxAgeSeconds;
    }

    @PostConstruct
    void validate() {
        if (shortMaxAgeSeconds < 1 || longMaxAgeSeconds < 1) {
            throw new IllegalArgumentException("Cache max-age durations must be positive");
        }
        if (config == null) {
            config = new EnumMap<>(CacheResource.class);
        }
    }

    public CachePolicy policy(CacheResource element) {
        CachePolicy configured = config.get(element);
        if (configured != null) {
            return configured;
        }
        return stableDefault(element);
    }

    /**
     * Defaults are deliberately suitable for the stable phase. Sensitive or
     * dynamic responses remain uncached, while versioned assets and public
     * information can use long-lived caching.
     */
    private CachePolicy stableDefault(CacheResource resource) {
        return switch (resource) {
            case BACKEND_ASSETS -> CachePolicy.NO_CACHE;
            case FRONTEND_VARIABLES -> CachePolicy.SHORT;
            case PUBLIC_INFORMATION -> CachePolicy.LONG;
            case PROJECT_DASHBOARD, PROJECT_DETAIL, FORM_METADATA, REFERENCE_DATA,
                    USER_ROLES, ACTION_AVAILABILITY, FEASIBILITY_STATISTICS,
                    PROJECT_RESULTS, PROJECT_DOCUMENTS, PROJECT_BRIDGEHEAD_EXECUTIONS,
                    NOTIFICATIONS, USER_DATA, AUTHENTICATED_GET,
                    MUTATION_RESPONSES -> CachePolicy.NO_STORE;
        };
    }

    public CacheControl cacheControl(CacheResource element) {
        return switch (policy(element)) {
            case NO_STORE -> CacheControl.noStore();
            case NO_CACHE -> CacheControl.noCache();
            case SHORT -> CacheControl.maxAge(Duration.ofSeconds(shortMaxAgeSeconds)).cachePublic();
            case LONG -> CacheControl.maxAge(Duration.ofSeconds(longMaxAgeSeconds)).cachePublic();
            case IMMUTABLE -> CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();
        };
    }

    @Bean
    public FilterRegistrationBean<CachePolicyFilter> cachePolicyFilter() {
        FilterRegistrationBean<CachePolicyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CachePolicyFilter(this));
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MAX_VALUE);
        return registration;
    }
}
