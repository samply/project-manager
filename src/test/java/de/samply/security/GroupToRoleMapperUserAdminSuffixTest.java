package de.samply.security;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GroupToRoleMapperUserAdminSuffixTest {
    // Separate user and admin groups below the bridgehead (/projects/dktk/bridgeheads/<group>/user, .../admin):
    // the admin group does not match the user pattern and must still give the admin role
    @Test
    void adminGroupWithItsOwnSuffixGivesTheAdminRole() {
        var roles = new UserOrganisationRoles();
        var sites = mock(BridgeheadsConfiguration.class);
        when(sites.getBridgeheadsForGroup("berlin")).thenReturn(List.of("berlin", "berlin-test"));
        when(sites.getBridgeheadsForGroup("essen")).thenReturn(List.of("essen"));
        var mapper = new GroupToRoleMapper(new ProjectManagerAdminGroups(List.of("projects/dktk/project-manager-admin")), sites);
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupPrefix", "/projects/dktk/bridgeheads/");
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupSuffix", "/user");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupPrefix", "/projects/dktk/bridgeheads/");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupSuffix", "/admin");

        assertThat(mapper.getRoleFromGroup("/projects/dktk/bridgeheads/berlin/admin", roles))
                .isEqualTo(OrganisationRole.BRIDGEHEAD_ADMIN);
        assertThat(roles.getBridgeheadRoles("berlin")).containsExactly(OrganisationRole.BRIDGEHEAD_ADMIN);
        assertThat(roles.getBridgeheadRoles("berlin-test")).containsExactly(OrganisationRole.BRIDGEHEAD_ADMIN);

        assertThat(mapper.getRoleFromGroup("/projects/dktk/bridgeheads/essen/user", roles))
                .isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(roles.getBridgeheadRoles("essen")).containsExactly(OrganisationRole.RESEARCHER);

        // Neither the bridgehead node itself nor other children (psp) give a role
        assertThat(mapper.getRoleFromGroup("/projects/dktk/bridgeheads/essen", roles)).isNull();
        assertThat(mapper.getRoleFromGroup("/projects/dktk/bridgeheads/essen/psp", roles)).isNull();
        assertThat(mapper.getRoleFromGroup("/projects/dktk/project-manager-admin", roles))
                .isEqualTo(OrganisationRole.PROJECT_MANAGER_ADMIN);
    }

    // Authentik names (DKTK_CCP_Berlin, DKTK_CCP_Berlin_Verwalter): the admin group matches both patterns
    @Test
    void adminGroupMatchingTheUserPatternTooGivesTheAdminRole() {
        var roles = new UserOrganisationRoles();
        var sites = mock(BridgeheadsConfiguration.class);
        when(sites.getBridgeheadsForGroup("berlin")).thenReturn(List.of("berlin"));
        var mapper = new GroupToRoleMapper(new ProjectManagerAdminGroups(List.of("admin-ccp-office")), sites);
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupPrefix", "DKTK_CCP_");
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupSuffix", "");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupPrefix", "DKTK_CCP_");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupSuffix", "_Verwalter");

        assertThat(mapper.getRoleFromGroup("DKTK_CCP_Berlin_Verwalter", roles)).isEqualTo(OrganisationRole.BRIDGEHEAD_ADMIN);
        assertThat(mapper.getRoleFromGroup("DKTK_CCP_Berlin", roles)).isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(roles.getBridgeheadRoles("berlin"))
                .containsExactlyInAnyOrder(OrganisationRole.BRIDGEHEAD_ADMIN, OrganisationRole.RESEARCHER);
    }
}
