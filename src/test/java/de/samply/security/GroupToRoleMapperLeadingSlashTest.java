package de.samply.security;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GroupToRoleMapperLeadingSlashTest {
    // Keycloak paths (CCE): the settings may be written with or without the leading slash.
    @ParameterizedTest
    @ValueSource(strings = {"", "/"})
    void groupSettingsMatchWithAndWithoutLeadingSlash(String slash) {
        var roles = new UserOrganisationRoles();
        var sites = mock(BridgeheadsConfiguration.class);
        when(sites.isRegisteredBridgehead("cce-gr")).thenReturn(true);
        var admins = new ProjectManagerAdminGroups(List.of(slash + "projects/cce/project-manager-admin", ""));
        var mapper = new GroupToRoleMapper(admins, sites);
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupPrefix", slash + "projects/cce/sites/");
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupSuffix", "");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupPrefix", slash + "projects/cce/sites/");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupSuffix", "/admin");
        ReflectionTestUtils.setField(mapper, "researcherGroups", List.of(slash + "apps/cce-federated-explorer"));

        assertThat(mapper.getRoleFromGroup("/projects/cce/project-manager-admin", roles))
                .isEqualTo(OrganisationRole.PROJECT_MANAGER_ADMIN);
        assertThat(mapper.getRoleFromGroup("/projects/cce/sites/cce-gr/admin", roles))
                .isEqualTo(OrganisationRole.BRIDGEHEAD_ADMIN);
        assertThat(roles.getBridgeheadRoles("cce-gr")).containsExactly(OrganisationRole.BRIDGEHEAD_ADMIN);
        assertThat(mapper.getRoleFromGroup("/apps/cce-federated-explorer", roles))
                .isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(mapper.getRoleFromGroup("/projects/cce/vdc", roles)).isNull();
        assertThat(admins.contains("")).isFalse();
    }
}
