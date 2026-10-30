package de.samply.security;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GroupToRoleMapper {

    /**
     * Name of the capturing group holding the bridgehead in {@code BK_USER_GROUP_REGEX} / {@code BK_ADMIN_GROUP_REGEX}.
     * Without it, the first capturing group is the bridgehead.
     */
    public static final String BRIDGEHEAD_REGEX_GROUP = "bridgehead";

    /**
     * Optional comma-separated identity-provider groups granting RESEARCHER access
     * independently of any registered bridgehead. Empty by default; matching is
     * exact and case-sensitive, ignoring whitespace and a leading slash ({@link GroupNames}).
     * This does not grant bridgehead membership or administrative privileges.
     */
    @Value(ProjectManagerConst.RESEARCHER_GROUPS_SV)
    private List<String> researcherGroups = List.of();

    // Regular expressions matched against the whole group (without leading slash); they capture the bridgehead.
    @Value(ProjectManagerConst.BK_USER_GROUP_REGEX_SV)
    private String bridgeheadUserGroupRegex = "";

    @Value(ProjectManagerConst.BK_ADMIN_GROUP_REGEX_SV)
    private String bridgeheadAdminGroupRegex = "";

    // Deprecated: only used while the corresponding regex is not set (prefix + bridgehead + suffix).
    @Value(ProjectManagerConst.BK_USER_GROUP_PREFIX_SV)
    private String bridgeheadUserGroupPrefix = "";

    @Value(ProjectManagerConst.BK_USER_GROUP_SUFFIX_SV)
    private String bridgeheadUserGroupSuffix = "";

    @Value(ProjectManagerConst.BK_ADMIN_GROUP_PREFIX_SV)
    private String bridgeheadAdminGroupPrefix = "";

    @Value(ProjectManagerConst.BK_ADMIN_GROUP_SUFFIX_SV)
    private String bridgeheadAdminGroupSuffix = "";

    private final ProjectManagerAdminGroups projectManagerAdminGroups;

    private final BridgeheadsConfiguration bridgeheadsConfiguration;

    private Pattern userGroupPattern;

    private Pattern adminGroupPattern;

    public GroupToRoleMapper(
            ProjectManagerAdminGroups projectManagerAdminGroups,
            BridgeheadsConfiguration bridgeheadsConfiguration) {
        this.projectManagerAdminGroups = projectManagerAdminGroups;
        this.bridgeheadsConfiguration = bridgeheadsConfiguration;
    }

    // Fails the start on a missing or invalid group pattern instead of at the first login.
    @PostConstruct
    void compileGroupPatterns() {
        userGroupPattern();
        adminGroupPattern();
    }

    public OrganisationRole getRoleFromGroup(String group, UserOrganisationRoles userOrganisationRoles) {
        group = GroupNames.normalize(group);
        OrganisationRole organisationRole = null;
        if (projectManagerAdminGroups.contains(group)) {
            organisationRole = OrganisationRole.PROJECT_MANAGER_ADMIN;
            userOrganisationRoles.addRoleNotDependentOnBridgehead(organisationRole);
        } else {
            // The admin pattern is checked first: the user pattern may match the admin groups too (DKTK_CCP_(.+)).
            Matcher matcher = adminGroupPattern().matcher(group);
            if (matcher.matches()) {
                organisationRole = addBridgeheadRole(matcher, OrganisationRole.BRIDGEHEAD_ADMIN, userOrganisationRoles);
            } else if ((matcher = userGroupPattern().matcher(group)).matches()) {
                organisationRole = addBridgeheadRole(matcher, OrganisationRole.RESEARCHER, userOrganisationRoles);
            }
        }
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

    // Returns null if the group names a bridgehead that is not registered.
    private OrganisationRole addBridgeheadRole(Matcher matcher, OrganisationRole organisationRole, UserOrganisationRoles userOrganisationRoles) {
        String bridgehead = extractBridgehead(matcher);
        if (!bridgeheadsConfiguration.isRegisteredBridgehead(bridgehead)) {
            return null;
        }
        userOrganisationRoles.addBridgeheadRole(bridgehead, organisationRole);
        return organisationRole;
    }

    private String extractBridgehead(Matcher matcher) {
        String bridgehead = matcher.pattern().namedGroups().containsKey(BRIDGEHEAD_REGEX_GROUP)
                ? matcher.group(BRIDGEHEAD_REGEX_GROUP) : matcher.group(1);
        return (bridgehead == null) ? "" : bridgehead.toLowerCase();
    }

    private Pattern userGroupPattern() {
        if (userGroupPattern == null) {
            userGroupPattern = compileGroupPattern(ProjectManagerConst.BK_USER_GROUP_REGEX, bridgeheadUserGroupRegex,
                    bridgeheadUserGroupPrefix, bridgeheadUserGroupSuffix);
        }
        return userGroupPattern;
    }

    private Pattern adminGroupPattern() {
        if (adminGroupPattern == null) {
            adminGroupPattern = compileGroupPattern(ProjectManagerConst.BK_ADMIN_GROUP_REGEX, bridgeheadAdminGroupRegex,
                    bridgeheadAdminGroupPrefix, bridgeheadAdminGroupSuffix);
        }
        return adminGroupPattern;
    }

    // The regex (or the prefix) is compared like the groups of the token, without a leading slash.
    private static Pattern compileGroupPattern(String variable, String regex, String prefix, String suffix) {
        String normalizedRegex = GroupNames.normalize(regex);
        if (normalizedRegex.isEmpty()) {
            if (GroupNames.normalize(prefix).isEmpty() && (suffix == null || suffix.isEmpty())) {
                throw new IllegalStateException(variable + " is not set");
            }
            normalizedRegex = Pattern.quote(GroupNames.normalize(prefix)) + "(.+)"
                    + ((suffix == null || suffix.isEmpty()) ? "" : Pattern.quote(suffix));
        }
        Pattern pattern = Pattern.compile(normalizedRegex);
        if (pattern.matcher("").groupCount() == 0) {
            throw new IllegalStateException(variable + " needs a capturing group for the bridgehead, e.g. (?<"
                    + BRIDGEHEAD_REGEX_GROUP + ">.+): " + normalizedRegex);
        }
        return pattern;
    }

}
