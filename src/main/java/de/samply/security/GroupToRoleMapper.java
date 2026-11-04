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

    // Comma-separated groups of the central approval committee (exact match, leading slash ignored); empty = nobody
    @Value(ProjectManagerConst.CENTRAL_APPROVAL_COMMITTEE_GROUPS_SV)
    private List<String> centralApprovalCommitteeGroups = List.of();

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
                // An admin group that does not match the user pattern (suffixes /user and /admin) is an admin group; one
                // that matches both (DKTK_CCP_Berlin_Verwalter with user prefix DKTK_CCP_ and an empty user suffix) is
                // one when the admin pattern is the longer one.
                if (organisationRole == null || isAdminOverUser())
                    organisationRole = OrganisationRole.BRIDGEHEAD_ADMIN;
            } else if (projectManagerAdminGroups.contains(group)) {
                organisationRole = OrganisationRole.PROJECT_MANAGER_ADMIN;
            } else if (centralApprovalCommitteeGroups.stream().map(GroupNames::normalize).anyMatch(group::equals)) {
                organisationRole = OrganisationRole.CENTRAL_APPROVAL_COMMITTEE;
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

    // The group names a site (BRIDGEHEADS_CONFIG_<ID>_GROUP, default the bridgehead id): the role goes to every
    // registered bridgehead of the site, e.g. the group dresden to dresden and dresden-test.
    private OrganisationRole addBridgheadToUserInfoAndFilterOrganisationRole(String group, OrganisationRole organisationRole, UserOrganisationRoles userOrganisationRoles) {
        if (organisationRole != null) {
            String siteGroup = switch (organisationRole) {
                case RESEARCHER -> extractSiteGroup(userPrefix(), bridgeheadUserGroupSuffix, group);
                case BRIDGEHEAD_ADMIN -> extractSiteGroup(adminPrefix(), bridgeheadAdminGroupSuffix, group);
                default -> null;
            };
            if (siteGroup == null) {
                userOrganisationRoles.addRoleNotDependentOnBridgehead(organisationRole);
            } else {
                List<String> bridgeheads = bridgeheadsConfiguration.getBridgeheadsForGroup(siteGroup);
                final OrganisationRole role = organisationRole;
                bridgeheads.forEach(bridgehead -> userOrganisationRoles.addBridgeheadRole(bridgehead, role));
                if (bridgeheads.isEmpty()) {
                    organisationRole = null;
                }
            }
        }
        return organisationRole;
    }

    private String extractSiteGroup(String prefix, String suffix, String group) {
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
