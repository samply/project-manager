package de.samply.security;

import de.samply.app.ProjectManagerConst;
import de.samply.user.roles.MethodRoles;
import de.samply.user.roles.RolesExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {


    @Value(ProjectManagerConst.APP_SECURITY_ENABLED_PROPERTY_SV)
    private boolean isSecurityEnabled;


    @Bean
    public OidcUserService customOidcUserService() {
        return new OidcProjectUserService();
    }

    @Order(1)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return (isSecurityEnabled) ?
                http.authorizeHttpRequests(this::addAuthorityMapping)
                        .oauth2Login(oauth2 -> oauth2
                                .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.oidcUserService(customOidcUserService())))
                        .build() :
                http.authorizeRequests(authorize -> authorize.requestMatchers("/**").permitAll()).build();
    }

    private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry addAuthorityMapping(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorization) {
        Map<String, MethodRoles> pathRolesMap = RolesExtractor.extractPathRolesMap();
        pathRolesMap.keySet().forEach(path -> authorization
                .requestMatchers(new AntPathRequestMatcher(path, pathRolesMap.get(path).httpMethod()))
                .hasAnyAuthority(pathRolesMap.get(path).roles()));
        return authorization.anyRequest().authenticated();
    }


}
