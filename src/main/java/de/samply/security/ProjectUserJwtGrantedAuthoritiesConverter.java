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
    private final String emailClaim;
    private final String firstNameClaim;
    private final String lastNameClaim;
    private final GrantedAuthoritiesExtractor grantedAuthoritiesExtractor;
    private final NewUsersImporter newUsersImporter;
    private final SessionUser sessionUser;

    public ProjectUserJwtGrantedAuthoritiesConverter(
            @Value(ProjectManagerConst.JWT_GROUPS_CLAIM_SV) String groupClaim,
            @Value(ProjectManagerConst.JWT_EMAIL_CLAIM_SV) String emailClaim,
            @Value(ProjectManagerConst.JWT_FIRST_NAME_CLAIM_SV) String firstNameClaim,
            @Value(ProjectManagerConst.JWT_LAST_NAME_CLAIM_SV) String lastNameClaim,
            GrantedAuthoritiesExtractor grantedAuthoritiesExtractor,
            NewUsersImporter newUsersImporter,
            SessionUser sessionUser) {
        this.groupClaim = groupClaim;
        this.emailClaim = emailClaim;
        this.firstNameClaim = firstNameClaim;
        this.lastNameClaim = lastNameClaim;
        this.grantedAuthoritiesExtractor = grantedAuthoritiesExtractor;
        this.newUsersImporter = newUsersImporter;
        this.sessionUser = sessionUser;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        sessionUser.setEmail(jwt.getClaim(emailClaim));
        sessionUser.setFirstName(jwt.getClaim(firstNameClaim));
        sessionUser.setLastName(jwt.getClaim(lastNameClaim));
        Collection<GrantedAuthority> grantedAuthorities = grantedAuthoritiesExtractor.extractAuthoritiesFromGroups(
                jwt.getClaimAsStringList(groupClaim));
        newUsersImporter.importNewUsers();
        return grantedAuthorities;
    }


}
