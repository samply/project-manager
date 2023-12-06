package de.samply.security;

import de.samply.user.OrganisationRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.oauth2.core.authorization.OAuth2AuthorizationManagers.hasScope;

@Configuration
@EnableWebSecurity
@Order(1)
public class SecurityConfiguration {

    @Autowired
    private GroupToRoleMapper groupToRoleMapper;

    @Value("${app.security.enabled}")
    private boolean isSecurityEnabled;


    @Bean
    public OidcUserService customOidcUserService() {
        return new OidcProjectUserService(groupToRoleMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return (isSecurityEnabled) ?
                http.authorizeHttpRequests(this::addPathAuthorityMapping)
                        .oauth2Login(oauth2 -> oauth2.userInfoEndpoint(
                                userInfoEndpointConfig -> userInfoEndpointConfig.oidcUserService(customOidcUserService())))
                        .build() :
                http.authorizeRequests(authorize -> authorize.requestMatchers("/**").permitAll()).build();
    }

    private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry addPathAuthorityMapping(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorization) {
        authorization.requestMatchers("/researcher-service").hasAuthority(OrganisationRole.RESEARCHER.name());
        return authorization.anyRequest().authenticated();
    }


}
