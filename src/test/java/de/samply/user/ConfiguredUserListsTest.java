package de.samply.user;

import de.samply.bridgehead.BridgeheadsConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// The addresses of the configuration: always in the email lists of their roles, removable only in the configuration
class ConfiguredUserListsTest {

    private final ConfiguredUserLists configuredUserLists = new ConfiguredUserLists(
            List.of(" Admin@Example.org", ""), List.of("committee@example.org"), bridgeheads());

    @Test
    void recognizesTheAddressesWithoutCase() {
        assertThat(configuredUserLists.isProjectManagerAdmin("Admin@Example.org")).isTrue();
        assertThat(configuredUserLists.isProjectManagerAdmin("other@example.org")).isFalse();
        assertThat(configuredUserLists.isCentralApprovalCommitteeMember("committee@example.org")).isTrue();
    }

    // A typo in the configuration stops the start, naming the variable
    @Test
    void refusesAMalformedAddress() {
        assertThatThrownBy(() -> new ConfiguredUserLists(List.of("admin@example"), List.of(), bridgeheads()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROJECT_MANAGER_ADMIN_EMAILS")
                .hasMessageContaining("admin@example");
    }

    // A site admin belongs to its site only
    @Test
    void recognizesTheSiteAdminsPerSite() {
        assertThat(configuredUserLists.isBridgeheadAdmin("site-admin@example.org", "jurassic-park")).isTrue();
        assertThat(configuredUserLists.isBridgeheadAdmin("site-admin@example.org", "atlantis")).isFalse();
    }

    // At startup every configured address is put into its list, normalized (empty entries are ignored); nothing is
    // removed
    @Test
    void loadsTheAddressesAtStartup() {
        UserService userService = mock(UserService.class);

        new ConfiguredUserListsImporter(configuredUserLists, userService).run(new DefaultApplicationArguments());

        verify(userService).createProjectManagerAdminUserIfNotExists("admin@example.org");
        verify(userService, never()).createProjectManagerAdminUserIfNotExists("");
        verify(userService).createCentralApprovalCommitteeUserIfNotExists("committee@example.org");
        verify(userService).createBridgeheadAdminUserIfNotExists("site-admin@example.org", "jurassic-park");
    }

    private static BridgeheadsConfiguration bridgeheads() {
        BridgeheadsConfiguration.BridgeheadConfig jurassicPark = new BridgeheadsConfiguration.BridgeheadConfig();
        jurassicPark.setAdminEmails(List.of("site-admin@example.org"));
        BridgeheadsConfiguration bridgeheadsConfiguration = new BridgeheadsConfiguration();
        bridgeheadsConfiguration.setConfig(Map.of("jurassic-park", jurassicPark, "atlantis",
                new BridgeheadsConfiguration.BridgeheadConfig()));
        return bridgeheadsConfiguration;
    }

}
