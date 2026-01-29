package de.samply.security;

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
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


@Configuration
@EnableScheduling
@Slf4j
public class JwtDecoderConfig {

    private final WebClient webClient;
    private final AtomicReference<JWKSet> jwkSetRef = new AtomicReference<>();
    private final String jwksUri;

    public JwtDecoderConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            WebClientFactory webClientFactory
    ) {
        if (issuerUri.endsWith("/")) {
            issuerUri = issuerUri.substring(0, issuerUri.length() - 1);
        }

        this.webClient = webClientFactory.createWebClient(issuerUri);

        JsonNode config = webClient.get()
                .uri("/.well-known/openid-configuration")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(5));

        this.jwksUri = Objects.requireNonNull(config).required("jwks_uri").asString();
        log.info("JWKS URI: {}", jwksUri);

        refreshJwks();
    }

    /**
     * Shared JWKSource for both Resource Server and OIDC Login
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        return (selector, _) -> selector.select(jwkSetRef.get());
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
     * Refresh JWKS every X ms (default 5 min)
     */
    @Scheduled(fixedDelayString = "${security.jwt.jwks-refresh-ms:300000}")
    public void refreshJwks() {
        try {
            String json = webClient.get()
                    .uri(jwksUri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(5));

            if (json != null && !json.isBlank()) {
                JWKSet jwkSet = JWKSet.parse(json);
                jwkSetRef.set(jwkSet);
                log.debug("JWKS refreshed, keys: {}", jwkSet.getKeys().size());
            }
        } catch (Exception e) {
            log.error("Failed to refresh JWKS", e);
        }
    }
}
