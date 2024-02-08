package de.samply.security;

import de.samply.app.ProjectManagerConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class OidcProjectUserService extends OidcUserService {


    private final SessionUser sessionUser;
    private final GrantedAuthoritiesExtractor grantedAuthoritiesExtractor;
    private final NewUsersImporter newUsersImporter;
    private final String groupClaim;

    public OidcProjectUserService(
            SessionUser sessionUser,
            GrantedAuthoritiesExtractor grantedAuthoritiesExtractor,
            NewUsersImporter newUsersImporter,
            @Value(ProjectManagerConst.JWT_GROUPS_CLAIM_PROPERTY_SV) String groupClaim) {
        this.sessionUser = sessionUser;
        this.grantedAuthoritiesExtractor = grantedAuthoritiesExtractor;
        this.newUsersImporter = newUsersImporter;
        this.groupClaim = groupClaim;
    }


    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        OidcIdToken idToken = oidcUser.getIdToken();
        OidcUserInfo userInfo = oidcUser.getUserInfo();
        sessionUser.setEmail(userInfo.getEmail());

        Collection<? extends GrantedAuthority> mappedAuthorities = extractAuthoritiesFromGroups(userInfo);
        newUsersImporter.importNewUsers();

        return new DefaultOidcUser(mappedAuthorities, idToken, userInfo);
    }

    public Collection<? extends GrantedAuthority> extractAuthoritiesFromGroups(OidcUserInfo userInfo) throws OAuth2AuthenticationException {
        Map<String, Object> claims = userInfo.getClaims();
        if (claims.containsKey(groupClaim)) {
            return grantedAuthoritiesExtractor.extractAuthoritiesFromGroups((Collection<String>) claims.get(groupClaim));
        }
        throw new OAuth2AuthenticationException("No groups found");
    }



}
