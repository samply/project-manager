package de.samply.security;

import de.samply.app.ProjectManagerConst;
import de.samply.user.OrganisationRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GroupToRoleMapper {

    @Value(ProjectManagerConst.BK_USER_GROUP_PREFIX_SV)
    private String bridgeheadUserGroupPrefix;

    @Value(ProjectManagerConst.BK_USER_GROUP_SUFFIX_SV)
    private String bridgeheadUserGroupSuffix;

    @Value(ProjectManagerConst.BK_ADMIN_GROUP_PREFIX_SV)
    private String bridgeheadAdminGroupPrefix;

    @Value(ProjectManagerConst.BK_ADMIN_GROUP_SUFFIX_SV)
    private String bridgeheadAdminGroupSuffix;

    @Autowired
    private ProjectManagerAdminGroups projectManagerAdminGroups;

    @Autowired
    private SessionUser sessionUser;

    private Boolean adminOverUser;


    private final Map<String, OrganisationRole> groupToRoleMapCache = new HashMap<>();

    public OrganisationRole getRoleFromGroup(String group) {
        group = removeSlashFromStart(group);
        OrganisationRole organisationRole = groupToRoleMapCache.get(group);
        if (organisationRole == null) {
            if (group.startsWith(bridgeheadUserGroupPrefix) && group.endsWith(bridgeheadUserGroupSuffix)) {
                organisationRole = OrganisationRole.RESEARCHER;
            }
            if (group.startsWith(bridgeheadAdminGroupPrefix) && group.endsWith(bridgeheadAdminGroupSuffix)) {
                if (organisationRole != null && isAdminOverUser())
                    organisationRole = OrganisationRole.BRIDGEHEAD_ADMIN;
            } else if (projectManagerAdminGroups.contains(group)) {
                organisationRole = OrganisationRole.PROJECT_MANAGER_ADMIN;
            }
            if (organisationRole != null) {
                groupToRoleMapCache.put(group, organisationRole);
            }
        }
        addBridgheadToUserInfo(group, organisationRole);
        return organisationRole;
    }

    private void addBridgheadToUserInfo(String group, OrganisationRole organisationRole) {
        if (organisationRole != null) {
            String bridgehead = switch (organisationRole) {
                case RESEARCHER -> extractBridgehead(bridgeheadUserGroupPrefix, bridgeheadUserGroupSuffix, group);
                case BRIDGEHEAD_ADMIN ->
                        extractBridgehead(bridgeheadAdminGroupPrefix, bridgeheadAdminGroupSuffix, group);
                default -> null;
            };
            sessionUser.setBridgehead(bridgehead);
            sessionUser.addOrganisationRole(organisationRole);
        }
    }

    private String extractBridgehead(String prefix, String suffix, String group) {
        return ((suffix.length() > 0) ? group.substring(prefix.length(), group.indexOf(suffix) - 1) : group.substring(prefix.length())).toLowerCase();
    }

    // Important for Keycloak configuration of the groups
    private String removeSlashFromStart(String group) {
        return (group.startsWith("/")) ? group.substring(1) : group;
    }

    private boolean isAdminOverUser() { // TODO: This logic works with the current configuration of DKTK. Please change it with something more reasonable.
        if (adminOverUser == null) {
            adminOverUser = (bridgeheadUserGroupPrefix + bridgeheadUserGroupSuffix).length() < (bridgeheadAdminGroupPrefix + bridgeheadAdminGroupSuffix).length();
        }
        return adminOverUser;
    }


}
