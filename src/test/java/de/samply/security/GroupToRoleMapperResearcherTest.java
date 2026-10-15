package de.samply.security;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GroupToRoleMapperResearcherTest {
    @Test
    void grantsOnlyResearcherToConfiguredExternalGroupsAndPreservesExistingRoles() {
        var roles = new UserOrganisationRoles();
        var sites = mock(BridgeheadsConfiguration.class);
        var admins = mock(ProjectManagerAdminGroups.class);
        var mapper = new GroupToRoleMapper(admins, sites);
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupPrefix", "SITE_");
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupSuffix", "");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupPrefix", "SITE_ADMIN_");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupSuffix", "");

        assertThat(mapper.getRoleFromGroup("external", roles)).isNull();
        ReflectionTestUtils.setField(mapper, "researcherGroups", List.of(" external ", "/another", ""));
        var authorities = new GrantedAuthoritiesExtractor(mapper)
                .extractAuthoritiesFromGroups(List.of("/external", "another"), roles);
        assertThat(authorities).hasSize(2).allSatisfy(authority ->
                assertThat(authority.getAuthority()).isEqualTo("RESEARCHER"));
        assertThat(roles.getRolesNotDependentOnBridgeheads()).containsExactly(OrganisationRole.RESEARCHER);
        assertThat(roles.getBridgeheads()).isEmpty();
        assertThat(mapper.getRoleFromGroup("External", roles)).isNull();
        assertThat(mapper.getRoleFromGroup("external-extra", roles)).isNull();
        assertThat(mapper.getRoleFromGroup("", roles)).isNull();
        assertThat(mapper.getRoleFromGroup("SITE_unknown", roles)).isNull();

        roles.getRolesNotDependentOnBridgeheads().clear();
        assertThat(mapper.getRoleFromGroup("external", roles)).isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(roles.containsRole(OrganisationRole.RESEARCHER)).isTrue();
        when(sites.isRegisteredBridgehead("alpha")).thenReturn(true);
        assertThat(mapper.getRoleFromGroup("SITE_alpha", roles)).isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(roles.getBridgeheadRoles("alpha")).containsExactly(OrganisationRole.RESEARCHER);
        when(admins.contains("office")).thenReturn(true);
        assertThat(mapper.getRoleFromGroup("office", roles)).isEqualTo(OrganisationRole.PROJECT_MANAGER_ADMIN);
        assertThat(mapper.getRoleFromGroup("SITE_ADMIN_alpha", roles)).isEqualTo(OrganisationRole.BRIDGEHEAD_ADMIN);
    }
}
