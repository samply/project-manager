package de.samply.security;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupToRoleMapper {

    /**
     * Optional comma-separated identity-provider groups granting RESEARCHER access
     * independently of any registered bridgehead. Empty by default; matching is
     * exact and case-sensitive, ignoring whitespace and a leading slash ({@link GroupNames}).
     * This does not grant bridgehead membership or administrative privileges.
     */
    @Value(ProjectManagerConst.RESEARCHER_GROUPS_SV)
    private List<String> researcherGroups = List.of();

    @Value(ProjectManagerConst.BK_USER_GROUP_PREFIX_SV)
    private String bridgeheadUserGroupPrefix;

    @Value(ProjectManagerConst.BK_USER_GROUP_SUFFIX_SV)
    private String bridgeheadUserGroupSuffix;

    @Value(ProjectManagerConst.BK_ADMIN_GROUP_PREFIX_SV)
    private String bridgeheadAdminGroupPrefix;

    @Value(ProjectManagerConst.BK_ADMIN_GROUP_SUFFIX_SV)
    private String bridgeheadAdminGroupSuffix;

    private final ProjectManagerAdminGroups projectManagerAdminGroups;

    private final BridgeheadsConfiguration bridgeheadsConfiguration;

    private Boolean adminOverUser;


    private final Map<String, OrganisationRole> groupToRoleMapCache = new ConcurrentHashMap<>();

    public GroupToRoleMapper(
            ProjectManagerAdminGroups projectManagerAdminGroups,
            BridgeheadsConfiguration bridgeheadsConfiguration) {
        this.projectManagerAdminGroups = projectManagerAdminGroups;
        this.bridgeheadsConfiguration = bridgeheadsConfiguration;
    }

    public OrganisationRole getRoleFromGroup(String group, UserOrganisationRoles userOrganisationRoles) {
        group = GroupNames.normalize(group);
        OrganisationRole organisationRole = groupToRoleMapCache.get(group);
        if (organisationRole == null) {
            if (group.startsWith(userPrefix()) && group.endsWith(bridgeheadUserGroupSuffix)) {
                organisationRole = OrganisationRole.RESEARCHER;
            }
            if (group.startsWith(adminPrefix()) && group.endsWith(bridgeheadAdminGroupSuffix)) {
                if (organisationRole != null && isAdminOverUser())
                    organisationRole = OrganisationRole.BRIDGEHEAD_ADMIN;
            } else if (projectManagerAdminGroups.contains(group)) {
                organisationRole = OrganisationRole.PROJECT_MANAGER_ADMIN;
            }
            if (organisationRole != null) {
                groupToRoleMapCache.put(group, organisationRole);
            }
        }
        organisationRole = addBridgheadToUserInfoAndFilterOrganisationRole(group, organisationRole, userOrganisationRoles);
        final String normalizedGroup = group;
        boolean globalResearcher = researcherGroups.stream()
                .map(GroupNames::normalize)
                .filter(value -> !value.isEmpty())
                .anyMatch(normalizedGroup::equals);
        if (globalResearcher) {
            userOrganisationRoles.addRoleNotDependentOnBridgehead(OrganisationRole.RESEARCHER);
            // Preserve existing administrative or bridgehead roles if a group overlaps.
            if (organisationRole == null) organisationRole = OrganisationRole.RESEARCHER;
        }
        return organisationRole;
    }

    private OrganisationRole addBridgheadToUserInfoAndFilterOrganisationRole(String group, OrganisationRole organisationRole, UserOrganisationRoles userOrganisationRoles) {
        if (organisationRole != null) {
            String bridgehead = switch (organisationRole) {
                case RESEARCHER -> extractBridgehead(userPrefix(), bridgeheadUserGroupSuffix, group);
                case BRIDGEHEAD_ADMIN -> extractBridgehead(adminPrefix(), bridgeheadAdminGroupSuffix, group);
                default -> null;
            };
            if (bridgehead == null) {
                userOrganisationRoles.addRoleNotDependentOnBridgehead(organisationRole);
            } else if (bridgeheadsConfiguration.isRegisteredBridgehead(bridgehead)) {
                userOrganisationRoles.addBridgeheadRole(bridgehead, organisationRole);
            } else {
                organisationRole = null;
            }
        }
        return organisationRole;
    }

    private String extractBridgehead(String prefix, String suffix, String group) {
        return ((!suffix.isEmpty()) ? group.substring(prefix.length(), group.indexOf(suffix)) : group.substring(prefix.length())).toLowerCase();
    }

    // Prefixes are compared like the groups of the token (without a leading slash); suffixes as configured.
    private String userPrefix() {
        return GroupNames.normalize(bridgeheadUserGroupPrefix);
    }

    private String adminPrefix() {
        return GroupNames.normalize(bridgeheadAdminGroupPrefix);
    }

    private boolean isAdminOverUser() { // TODO: This logic works with the current configuration of DKTK. Please change it with something more reasonable. (@The stressed developer)
        if (adminOverUser == null) {
            adminOverUser = (userPrefix() + bridgeheadUserGroupSuffix).length() < (adminPrefix() + bridgeheadAdminGroupSuffix).length();
        }
        return adminOverUser;
    }


}
