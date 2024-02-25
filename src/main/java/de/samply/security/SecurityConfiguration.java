package de.samply.security;

import de.samply.app.ProjectManagerConst;
import de.samply.frontend.FrontendConfiguration;
import de.samply.user.roles.MethodRoles;
import de.samply.user.roles.RolesExtractor;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    private RequestCacheFilter requestCacheFilter;

    @Value(ProjectManagerConst.SECURITY_ENABLED_SV)
    private boolean isSecurityEnabled;

    @Value(ProjectManagerConst.EXPLORER_URL_SV)
    private String explorerUrl;

    @Value(ProjectManagerConst.JWKS_URI_PROPERTY_SV)
    private String jwksUri;

    @Autowired
    private FrontendConfiguration frontendConfiguration;

    @Autowired
    private ProjectUserJwtGrantedAuthoritiesConverter projectUserJwtGrantedAuthoritiesConverter;

    @Autowired
    private OidcProjectUserService oidcProjectUserService;

    @Order(1)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return (isSecurityEnabled) ?
                http.addFilterBefore(requestCacheFilter, OAuth2LoginAuthenticationFilter.class)
                        .authorizeHttpRequests(this::addAuthorityMapping)
                        .cors(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable())
                        .oauth2ResourceServer(resourceServerConfigurer ->
                                resourceServerConfigurer.jwt(jwtConfigurer -> {
                                    jwtConfigurer.jwkSetUri(jwksUri);
                                    jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter());
                                }))
                        .oauth2Login(oauth2 -> oauth2
                                .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.oidcUserService(oidcProjectUserService))
                                .successHandler(successHandler()))
                        .build() :
                http.authorizeRequests(authorize -> authorize.requestMatchers("/**").permitAll()).build();
    }

    private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry addAuthorityMapping(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorization) {
        // Services without authentication required
        authorization.requestMatchers(new AntPathRequestMatcher(ProjectManagerConst.INFO, "GET")).permitAll();
        // Services with authentication required
        Map<String, MethodRoles> pathRolesMap = RolesExtractor.extractPathRolesMap();
        pathRolesMap.keySet().forEach(path -> authorization
                .requestMatchers(new AntPathRequestMatcher(path, pathRolesMap.get(path).httpMethod()))
                .hasAnyAuthority(pathRolesMap.get(path).roles()));
        return authorization.anyRequest().authenticated();
    }

    private AuthenticationSuccessHandler successHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            private RequestCache requestCache = new HttpSessionRequestCache();

            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                SavedRequest savedRequest = requestCache.getRequest(request, response);
                setUseReferer(true);
                if (savedRequest != null) {
                    String targetUrl = savedRequest.getRedirectUrl();
                    getRedirectStrategy().sendRedirect(request, response, targetUrl);
                } else {
                    super.onAuthenticationSuccess(request, response, authentication);
                }
            }
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(projectUserJwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }


    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(frontendConfiguration.getBaseUrl(), explorerUrl));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization")); // Allow required headers
        configuration.setAllowCredentials(true); // Allow credentials
        configuration.setExposedHeaders(Arrays.asList(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, HttpHeaders.CONTENT_DISPOSITION)); // Expose required headers
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
