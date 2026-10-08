package de.samply.security;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import de.samply.utils.WebClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;


@Configuration
@EnableScheduling
@Slf4j
public class JwtDecoderConfig {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_RETRIES = 2;
    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(1);

    private final WebClient webClient;
    private final AtomicReference<JWKSet> jwkSetRef = new AtomicReference<>();
    private volatile String jwksUri;

    public JwtDecoderConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            WebClientFactory webClientFactory
    ) {
        if (issuerUri.endsWith("/")) {
            issuerUri = issuerUri.substring(0, issuerUri.length() - 1);
        }

        this.webClient = webClientFactory.createWebClient(issuerUri);

        // Don't fail startup if the SSO is temporarily unavailable: keys are fetched lazily on first use
        // and by the scheduled refresh.
        refreshJwks();
    }

    /**
     * Shared JWKSource for both Resource Server and OIDC Login
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        return (selector, _) -> {
            JWKSet jwkSet = jwkSetRef.get();
            if (jwkSet == null) {
                refreshJwks();
                jwkSet = jwkSetRef.get();
            }
            if (jwkSet == null) {
                throw new KeySourceException("JWKS not available (SSO unreachable)");
            }
            return selector.select(jwkSet);
        };
    }

    /**
     * Resource-server JwtDecoder
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return NimbusJwtDecoder.withJwkSource(jwkSource).build();
    }

    /**
     * IMPORTANT:
     * Override the decoder used for ID Token validation (OIDC login)
     * This forces Spring Security to use YOUR cached JWKS instead of
     * fetching it again over the network (which causes the timeout).
     */
    @Bean
    public JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory(JWKSource<SecurityContext> jwkSource) {
        return _ -> NimbusJwtDecoder.withJwkSource(jwkSource).build();
    }

    /**
     * Refresh JWKS every X ms (default 5 min).
     * On failure the previously cached JWKS is kept.
     */
    @Scheduled(fixedDelayString = "${security.jwt.jwks-refresh-ms:300000}")
    public synchronized void refreshJwks() {
        try {
            if (jwksUri == null) {
                JsonNode config = fetch("/.well-known/openid-configuration", JsonNode.class);
                jwksUri = Objects.requireNonNull(config).required("jwks_uri").asString();
                log.info("JWKS URI: {}", jwksUri);
            }

            String json = fetch(jwksUri, String.class);
            if (json != null && !json.isBlank()) {
                JWKSet jwkSet = JWKSet.parse(json);
                jwkSetRef.set(jwkSet);
                log.debug("JWKS refreshed, keys: {}", jwkSet.getKeys().size());
            }
        } catch (Exception e) {
            String cacheState = jwkSetRef.get() != null ? "keeping cached JWKS" : "no JWKS cached yet";
            if (isTransient(e)) {
                log.warn("Failed to refresh JWKS ({}): {}", cacheState, e.getMessage());
            } else {
                log.error("Failed to refresh JWKS ({})", cacheState, e);
            }
        }
    }

    private <T> T fetch(String uri, Class<T> type) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(type)
                .timeout(REQUEST_TIMEOUT)
                .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                        .filter(JwtDecoderConfig::isTransient)
                        .onRetryExhaustedThrow((_, signal) -> signal.failure()))
                .block();
    }

    private static boolean isTransient(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof WebClientResponseException responseException) {
                return responseException.getStatusCode().is5xxServerError();
            }
            if (cause instanceof WebClientRequestException || cause instanceof TimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
