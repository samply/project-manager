package de.samply.security;

import de.samply.user.OrganisationRole;
import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Getter
public class SessionUserInfo {

    private Optional<String> bridgehead = Optional.empty();
    private final Set<OrganisationRole> organisationRoles = new HashSet<>();

    public void setBridgehead(String bridgehead) {
        if (bridgehead != null && this.bridgehead.isEmpty()) {
            this.bridgehead = Optional.of(bridgehead);
        }
    }

    public void addOrganisationRole(OrganisationRole organisationRole) {
        if (organisationRole != null) {
            organisationRoles.add(organisationRole);
        }
    }
}
