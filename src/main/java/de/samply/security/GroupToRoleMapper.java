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


    private final Map<String, OrganisationRole> groupToRoleMapCache = new HashMap<>();

    public OrganisationRole getRoleFromGroup(String group) {
        OrganisationRole organisationRole = groupToRoleMapCache.get(group);
        if (organisationRole == null) {
            if (group.startsWith(bridgeheadUserGroupPrefix) && group.endsWith(bridgeheadUserGroupSuffix)) {
                organisationRole = OrganisationRole.RESEARCHER;
            } else if (group.startsWith(bridgeheadAdminGroupPrefix) && group.endsWith(bridgeheadAdminGroupSuffix)) {
                organisationRole = OrganisationRole.BRIDGEHEAD_ADMIN;
            } else if (projectManagerAdminGroups.contains(group)) {
                organisationRole = OrganisationRole.PROJECT_MANAGER_ADMIN;
            }
            if (organisationRole != null) {
                groupToRoleMapCache.put(group, organisationRole);
            }
        }
        return organisationRole;
    }

}
