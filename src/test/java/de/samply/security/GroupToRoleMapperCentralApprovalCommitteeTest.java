package de.samply.security;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GroupToRoleMapperCentralApprovalCommitteeTest {
    // CCE: /projects/cce/central-committee; the setting may be written with or without the leading slash
    @ParameterizedTest
    @ValueSource(strings = {"", "/"})
    void mapsTheCommitteeGroupToANonBridgeheadRole(String slash) {
        var roles = new UserOrganisationRoles();
        var mapper = mapper(List.of(slash + "projects/cce/central-committee"));

        assertThat(mapper.getRoleFromGroup("/projects/cce/central-committee", roles))
                .isEqualTo(OrganisationRole.CENTRAL_APPROVAL_COMMITTEE);
        assertThat(roles.getRolesNotDependentOnBridgeheads()).containsExactly(OrganisationRole.CENTRAL_APPROVAL_COMMITTEE);
        assertThat(roles.getBridgeheads()).isEmpty();
        assertThat(mapper.getRoleFromGroup("/projects/cce/central-committee-extra", roles)).isNull();
    }

    // DKTK: setting unset, nobody has the role
    @Test
    void nobodyIsCommitteeWithoutTheSetting() {
        var roles = new UserOrganisationRoles();
        var mapper = mapper(List.of());

        assertThat(mapper.getRoleFromGroup("/projects/cce/central-committee", roles)).isNull();
        assertThat(roles.getRolesNotDependentOnBridgeheads()).isEmpty();
    }

    private GroupToRoleMapper mapper(List<String> committeeGroups) {
        var mapper = new GroupToRoleMapper(new ProjectManagerAdminGroups(List.of("/projects/cce/project-manager-admin")),
                mock(BridgeheadsConfiguration.class));
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupPrefix", "/projects/cce/sites/");
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupSuffix", "");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupPrefix", "/projects/cce/sites/");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupSuffix", "/admin");
        ReflectionTestUtils.setField(mapper, "centralApprovalCommitteeGroups", committeeGroups);
        return mapper;
    }
}
