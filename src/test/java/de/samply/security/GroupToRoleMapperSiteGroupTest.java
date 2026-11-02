package de.samply.security;
import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GroupToRoleMapperSiteGroupTest {
    // One site group (BRIDGEHEADS_CONFIG_<ID>_GROUP) gives the role on every bridgehead of the site
    @Test
    void siteGroupGivesTheRoleOnEveryBridgeheadOfTheSite() {
        var roles = new UserOrganisationRoles();
        var sites = mock(BridgeheadsConfiguration.class);
        when(sites.getBridgeheadsForGroup("berlin")).thenReturn(List.of("berlin", "berlin-test"));
        when(sites.getBridgeheadsForGroup("essen")).thenReturn(List.of("essen"));
        var mapper = new GroupToRoleMapper(new ProjectManagerAdminGroups(List.of("projects/dktk/project-manager-admin")), sites);
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupPrefix", "/projects/dktk/sites/");
        ReflectionTestUtils.setField(mapper, "bridgeheadUserGroupSuffix", "");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupPrefix", "/projects/dktk/sites/");
        ReflectionTestUtils.setField(mapper, "bridgeheadAdminGroupSuffix", "/admin");

        assertThat(mapper.getRoleFromGroup("/projects/dktk/sites/berlin/admin", roles))
                .isEqualTo(OrganisationRole.BRIDGEHEAD_ADMIN);
        assertThat(roles.getBridgeheadRoles("berlin")).containsExactly(OrganisationRole.BRIDGEHEAD_ADMIN);
        assertThat(roles.getBridgeheadRoles("berlin-test")).containsExactly(OrganisationRole.BRIDGEHEAD_ADMIN);

        assertThat(mapper.getRoleFromGroup("/projects/dktk/sites/essen", roles)).isEqualTo(OrganisationRole.RESEARCHER);
        assertThat(roles.getBridgeheadRoles("essen")).containsExactly(OrganisationRole.RESEARCHER);

        // A site without a registered bridgehead on this server gives no role
        assertThat(mapper.getRoleFromGroup("/projects/dktk/sites/goettingen", roles)).isNull();
        assertThat(roles.getBridgeheads()).containsExactlyInAnyOrder("berlin", "berlin-test", "essen");
    }
}
