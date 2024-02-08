package de.samply.security;

import de.samply.app.ProjectManagerConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class ProjectUserJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String groupClaim;
    private final GrantedAuthoritiesExtractor grantedAuthoritiesExtractor;
    private final NewUsersImporter newUsersImporter;
    private final SessionUser sessionUser;

    public ProjectUserJwtGrantedAuthoritiesConverter(
            @Value(ProjectManagerConst.JWT_GROUPS_CLAIM_PROPERTY_SV) String groupClaim,
            GrantedAuthoritiesExtractor grantedAuthoritiesExtractor,
            NewUsersImporter newUsersImporter,
            SessionUser sessionUser) {
        this.groupClaim = groupClaim;
        this.grantedAuthoritiesExtractor = grantedAuthoritiesExtractor;
        this.newUsersImporter = newUsersImporter;
        this.sessionUser = sessionUser;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        sessionUser.setEmail(jwt.getClaim(ProjectManagerConst.OIDC_EMAIL_CLAIM));
        Collection<GrantedAuthority> grantedAuthorities = grantedAuthoritiesExtractor.extractAuthoritiesFromGroups(
                jwt.getClaimAsStringList(groupClaim));
        newUsersImporter.importNewUsers();
        return grantedAuthorities;
    }


}
