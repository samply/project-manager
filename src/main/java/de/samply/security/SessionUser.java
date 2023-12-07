package de.samply.security;

import de.samply.user.OrganisationRole;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SessionUser {

    private final Map<String, Set<OrganisationRole>> bridgheadRolesMap = new HashMap<>();
    private final Set<OrganisationRole> organisationRolesNotDependentOnBridgeheads = new HashSet<>();
    private String email;

    public void addBridgeheadRole(String bridgehead, OrganisationRole role) {
        if (bridgehead != null && role != null) {
            Set<OrganisationRole> bridgeheadRoles = bridgheadRolesMap.get(bridgehead);
            if (bridgeheadRoles == null) {
                bridgeheadRoles = new HashSet<>();
                bridgheadRolesMap.put(bridgehead, bridgeheadRoles);
            }
            bridgeheadRoles.add(role);
        }
    }

    public void addOrganisationRoleNotDependentOnBridgehead(OrganisationRole     organisationRole) {
        if (organisationRole != null) {
            organisationRolesNotDependentOnBridgeheads.add(organisationRole);
        }
    }

    public void setEmail(String email) {
        if (email != null) {
            this.email = email;
        }
    }

    public Set<OrganisationRole> getOrganisationRolesNotDependentOnBridgeheads() {
        return organisationRolesNotDependentOnBridgeheads;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getBridgeheads() {
        return bridgheadRolesMap.keySet();
    }

    public Optional<Set<OrganisationRole>> getBridgeheadRoles(String bridgehead) {
        return Optional.of(bridgheadRolesMap.get(bridgehead));
    }

}
