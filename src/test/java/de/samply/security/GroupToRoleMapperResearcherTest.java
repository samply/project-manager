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
        var user = mock(SessionUser.class);
        var roles = new UserOrganisationRoles();
        when(user.getUserOrganisationRoles()).thenReturn(roles);
        var sites = mock(BridgeheadsConfiguration.class);
        var admins = mock(ProjectManagerAdminGroups.class);
        var mapper = new GroupToRoleMapper(admins, user, sites);
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupPrefix", "SITE_");
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupSuffix", "");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupPrefix", "SITE_ADMIN_");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupSuffix", "");

        assertThat(mapper.getRoleFromGroup("external")).isNull();
        ReflectionTestUtils.setField(mapper, "researcherGroups", List.of(" external ", "/another", ""));
        var authorities = new GrantedAuthoritiesExtractor(mapper)
                .extractAuthoritiesFromGroups(List.of("/external", "another"));
        assertThat(authorities).hasSize(2).allSatisfy(authority ->
                assertThat(authority.getAuthority()).isEqualTo("RESEARCHER"));
        assertThat(roles.getRolesNotDependentOnBridgeheads()).containsExactly(OrganisationRole.RESEARCHER);
        assertThat(roles.getBridgeheads()).isEmpty();
        assertThat(mapper.getRoleFromGroup("External")).isNull();
        assertThat(mapper.getRoleFromGroup("external-extra")).isNull();
        assertThat(mapper.getRoleFromGroup("")).isNull();
        assertThat(mapper.getRoleFromGroup("SITE_unknown")).isNull();

        roles.getRolesNotDependentOnBridgeheads().clear();
        assertThat(mapper.getRoleFromGroup("external")).isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(roles.containsRole(OrganisationRole.RESEARCHER)).isTrue();
        when(sites.isRegisteredBridgehead("alpha")).thenReturn(true);
        assertThat(mapper.getRoleFromGroup("SITE_alpha")).isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(roles.getBridgeheadRoles("alpha")).containsExactly(OrganisationRole.RESEARCHER);
        when(admins.contains("office")).thenReturn(true);
        assertThat(mapper.getRoleFromGroup("office")).isEqualTo(OrganisationRole.PROJECT_MANAGER_ADMIN);
        assertThat(mapper.getRoleFromGroup("SITE_ADMIN_alpha")).isEqualTo(OrganisationRole.BRIDGEHEAD_ADMIN);
    }
}
