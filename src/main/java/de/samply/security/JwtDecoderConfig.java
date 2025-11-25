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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.time.Duration;
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

        // Load OIDC discovery
        JsonNode config = webClient.get()
                .uri("/.well-known/openid-configuration")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(5));

        this.jwksUri = config.get("jwks_uri").asText();
        log.info("JWKS URI: {}", jwksUri);

        // Initial load of JWKS
        refreshJwks();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        JWKSource<SecurityContext> jwkSource = (jwkSelector, context) ->
                jwkSelector.select(jwkSetRef.get());

        return NimbusJwtDecoder.withJwkSource(jwkSource).build();
    }

    /**
     * Refresh JWKS periodically (every 5 minutes)
     */
    @Scheduled(fixedDelayString = "${security.jwt.jwks-refresh-ms:300000}")
    public void refreshJwks() {
        try {
            // Fetch JWKS as String
            String jwksJson = webClient.get()
                    .uri(URI.create(jwksUri))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(5));

            if (jwksJson != null && !jwksJson.isBlank()) {
                // Parse with Nimbus JWKSet
                JWKSet jwkSet = JWKSet.parse(jwksJson);

                if (!jwkSet.getKeys().isEmpty()) {
                    jwkSetRef.set(jwkSet);
                    log.debug("JWKS refreshed successfully, keys found: {}", jwkSet.getKeys().size());
                } else {
                    log.warn("JWKS refresh: no keys found!");
                }
            } else {
                log.warn("JWKS refresh returned empty body!");
            }

        } catch (Exception e) {
            log.error("Failed to refresh JWKS", e);
        }
    }
}
