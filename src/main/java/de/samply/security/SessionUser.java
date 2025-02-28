package de.samply.security;

import de.samply.app.ProjectManagerConst;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Data
public class SessionUser {

    private String email;
    private String firstName;
    private String lastName;
    private UserOrganisationRoles userOrganisationRoles = new UserOrganisationRoles();

    public SessionUser(@Value(ProjectManagerConst.SECURITY_ENABLED_SV) boolean isSecurityEnabled) {
        if (!isSecurityEnabled) {
            setEmail(ProjectManagerConst.TEST_EMAIL);
            userOrganisationRoles.addRoleNotDependentOnBridgehead(OrganisationRole.PROJECT_MANAGER_ADMIN);
            userOrganisationRoles.addBridgeheadRole(ProjectManagerConst.TEST_BRIDGEHEAD, OrganisationRole.RESEARCHER);
            userOrganisationRoles.addBridgeheadRole(ProjectManagerConst.TEST_BRIDGEHEAD, OrganisationRole.BRIDGEHEAD_ADMIN);
        }
    }

    public Set<String> getBridgeheads() {
        return getUserOrganisationRoles().getBridgeheads();
    }

}
