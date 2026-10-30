package de.samply.security;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GroupToRoleMapperRegexTest {

    private GroupToRoleMapper mapper(String... settings) {
        var sites = mock(BridgeheadsConfiguration.class);
        when(sites.isRegisteredBridgehead("alpha")).thenReturn(true);
        var mapper = new GroupToRoleMapper(new ProjectManagerAdminGroups(List.of("office")), sites);
        for (int i = 0; i < settings.length; i += 2) ReflectionTestUtils.setField(mapper, settings[i], settings[i + 1]);
        return mapper;
    }

    // DKTK (Authentik): the user pattern also matches the admin groups; the admin pattern wins.
    @Test
    void userPatternMatchingAdminGroupsToo() {
        var mapper = mapper("bridgeheadUserGroupRegex", "DKTK_CCP_(.+)",
                "bridgeheadAdminGroupRegex", "DKTK_CCP_(.+)_Verwalter");
        var roles = new UserOrganisationRoles();
        assertThat(mapper.getRoleFromGroup("DKTK_CCP_Alpha_Verwalter", roles)).isEqualTo(OrganisationRole.BRIDGEHEAD_ADMIN);
        assertThat(mapper.getRoleFromGroup("DKTK_CCP_alpha", roles)).isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(roles.getBridgeheadRoles("alpha"))
                .containsExactlyInAnyOrder(OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.RESEARCHER);
        assertThat(mapper.getRoleFromGroup("DKTK_CCP_beta", roles)).isNull();
        assertThat(mapper.getRoleFromGroup("office", roles)).isEqualTo(OrganisationRole.PROJECT_MANAGER_ADMIN);
    }

    // Structures that prefix + suffix cannot express: the site in the middle, other parts around it.
    @Test
    void bridgeheadAnywhereInTheGroup() {
        var mapper = mapper("bridgeheadUserGroupRegex", "(?:researchers|users)\\.(?<bridgehead>[a-z]+)\\.(?:prod|test)",
                "bridgeheadAdminGroupRegex", "admins-(?<bridgehead>[a-z]+)-[0-9]+");
        var roles = new UserOrganisationRoles();
        assertThat(mapper.getRoleFromGroup("users.alpha.test", roles)).isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(mapper.getRoleFromGroup("admins-alpha-42", roles)).isEqualTo(OrganisationRole.BRIDGEHEAD_ADMIN);
        assertThat(mapper.getRoleFromGroup("users.alpha", roles)).isNull();
        assertThat(mapper.getRoleFromGroup("x-admins-alpha-42", roles)).isNull();
    }

    @Test
    void legacyPrefixAndSuffixWithoutRegex() {
        var mapper = mapper("bridgeheadUserGroupPrefix", "DKTK_CCP_", "bridgeheadAdminGroupPrefix", "DKTK_CCP_",
                "bridgeheadAdminGroupSuffix", "_Verwalter");
        var roles = new UserOrganisationRoles();
        assertThat(mapper.getRoleFromGroup("DKTK_CCP_alpha_Verwalter", roles)).isEqualTo(OrganisationRole.BRIDGEHEAD_ADMIN);
        assertThat(mapper.getRoleFromGroup("DKTK_CCP_alpha", roles)).isEqualTo(OrganisationRole.RESEARCHER);
        // The prefix is literal, not a regex.
        assertThat(mapper("bridgeheadUserGroupPrefix", "a.b_", "bridgeheadAdminGroupRegex", "x(.+)")
                .getRoleFromGroup("aXb_alpha", new UserOrganisationRoles())).isNull();
    }

    @Test
    void regexTakesPrecedenceOverPrefixAndSuffix() {
        var mapper = mapper("bridgeheadUserGroupRegex", "new_(.+)", "bridgeheadUserGroupPrefix", "old_",
                "bridgeheadAdminGroupRegex", "admin_(.+)");
        assertThat(mapper.getRoleFromGroup("new_alpha", new UserOrganisationRoles())).isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(mapper.getRoleFromGroup("old_alpha", new UserOrganisationRoles())).isNull();
    }

    @Test
    void missingOrInvalidPatternFailsTheStart() {
        assertThatThrownBy(() -> mapper("bridgeheadUserGroupRegex", "u_(.+)").compileGroupPatterns())
                .hasMessageContaining("BK_ADMIN_GROUP_REGEX");
        assertThatThrownBy(() -> mapper("bridgeheadUserGroupRegex", "u_.+", "bridgeheadAdminGroupRegex", "a_(.+)")
                .compileGroupPatterns()).hasMessageContaining("capturing group");
    }
}
