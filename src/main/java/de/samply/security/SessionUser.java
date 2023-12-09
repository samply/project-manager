package de.samply.security;

import de.samply.app.ProjectManagerConst;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SessionUser {

    private String email;
    private UserOrganisationRoles userOrganisationRoles = new UserOrganisationRoles();

    public SessionUser(@Value(ProjectManagerConst.APP_SECURITY_ENABLED_PROPERTY_SV) boolean isSecurityEnabled) {
        if (!isSecurityEnabled) {
            setEmail(ProjectManagerConst.TEST_EMAIL);
            userOrganisationRoles.addRoleNotDependentOnBridgehead(OrganisationRole.PROJECT_MANAGER_ADMIN);
            userOrganisationRoles.addBridgeheadRole(ProjectManagerConst.TEST_BRIDGEHEAD, OrganisationRole.RESEARCHER);
            userOrganisationRoles.addBridgeheadRole(ProjectManagerConst.TEST_BRIDGEHEAD, OrganisationRole.BRIDGEHEAD_ADMIN);
        }
    }

    public void setEmail(String email) {
        if (email != null) {
            this.email = email;
        }
    }

    public String getEmail() {
        return email;
    }

    public UserOrganisationRoles getUserOrganisationRoles() {
        return userOrganisationRoles;
    }

    public Set<String> getBridgeheads() {
        return getUserOrganisationRoles().getBridgeheads();
    }

}
