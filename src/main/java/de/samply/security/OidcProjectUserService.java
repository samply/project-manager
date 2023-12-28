package de.samply.security;

import de.samply.app.ProjectManagerConst;
import de.samply.user.UserService;
import de.samply.user.roles.OrganisationRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import java.util.stream.Collectors;

@Service
public class OidcProjectUserService extends OidcUserService {

    @Value(ProjectManagerConst.JWT_GROUPS_CLAIM_PROPERTY_SV)
    private String groupClaim;

    @Autowired
    private SessionUser sessionUser;

    @Autowired
    private GroupToRoleMapper groupToRoleMapper;

    @Autowired
    private UserService userService;


    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        OidcIdToken idToken = oidcUser.getIdToken();
        OidcUserInfo userInfo = oidcUser.getUserInfo();
        sessionUser.setEmail(userInfo.getEmail());

        Collection<? extends GrantedAuthority> mappedAuthorities = extractAuthoritiesFromGroups(userInfo);
        importNewUsers();

        return new DefaultOidcUser(mappedAuthorities, idToken, userInfo);
    }

    private Collection<? extends GrantedAuthority> extractAuthoritiesFromGroups(OidcUserInfo userInfo) throws OAuth2AuthenticationException {
        Map<String, Object> claims = userInfo.getClaims();
        if (claims.containsKey(groupClaim)) {
            return ((Collection<String>) claims.get(groupClaim)).stream()
                    .map(groupToRoleMapper::getRoleFromGroup)
                    .filter(role -> role != null)
                    .map(role -> new SimpleGrantedAuthority(role.name()))// change me
                    .collect(Collectors.toList());
        }
        throw new OAuth2AuthenticationException("No groups found");
    }

    private void importNewUsers() {
        if (sessionUser.getUserOrganisationRoles().getRolesNotDependentOnBridgeheads().contains(OrganisationRole.PROJECT_MANAGER_ADMIN)) {
            userService.createProjectManagerAdminUserIfNotExists(sessionUser.getEmail());
        }
        sessionUser.getBridgeheads().forEach(bridgehead -> {
            if (sessionUser.getUserOrganisationRoles().getBridgeheadRoles(bridgehead).contains(OrganisationRole.BRIDGEHEAD_ADMIN)) {
                userService.createBridgeheadAdminUserIfNotExists(sessionUser.getEmail(), bridgehead);
            }
        });
    }

}
