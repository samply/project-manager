package de.samply.user;

import de.samply.bridgehead.BridgeheadsConfiguration;
import de.samply.db.model.BridgeheadAdminUser;
import de.samply.db.model.CentralApprovalCommitteeUser;
import de.samply.db.model.ProjectManagerAdminUser;
import de.samply.db.model.User;
import de.samply.frontend.dto.DtoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// The email lists of the configuration page: the name comes from the user table once the person has logged in
class DtoUserServiceUserListsTest {

    private final UserService userService = mock(UserService.class);
    private final BridgeheadsConfiguration bridgeheadsConfiguration = mock(BridgeheadsConfiguration.class);
    private final ConfiguredUserLists configuredUserLists = mock(ConfiguredUserLists.class);
    private DtoUserService dtoUserService;

    @BeforeEach
    void setUp() {
        when(userService.fetchUserIgnoringCase(any())).thenReturn(Optional.empty());
        when(userService.fetchUserIgnoringCase("zoe@example.org"))
                .thenReturn(Optional.of(new User(1L, "zoe@example.org", "Zoe", "Zimmer", false)));
        dtoUserService = new DtoUserService(userService, mock(DtoFactory.class), bridgeheadsConfiguration, configuredUserLists);
    }

    // Sorted by name; someone who has not logged in yet has no name and is sorted by the address
    @Test
    void listsTheCommitteeMembersWithTheirNames() {
        when(userService.fetchAllCentralApprovalCommitteeUsers()).thenReturn(List.of(
                new CentralApprovalCommitteeUser(1L, "zoe@example.org"),
                new CentralApprovalCommitteeUser(2L, "new@example.org")));

        List<de.samply.frontend.dto.User> members = dtoUserService.fetchCentralApprovalCommitteeMembers();

        assertThat(members).extracting(de.samply.frontend.dto.User::email)
                .containsExactly("new@example.org", "zoe@example.org");
        assertThat(members.get(1).firstName()).isEqualTo("Zoe");
        assertThat(members.get(0).firstName()).isNull();
    }

    // An address of the configuration is marked: the configuration page cannot remove it
    @Test
    void marksTheAddressesOfTheConfiguration() {
        when(configuredUserLists.isProjectManagerAdmin("zoe@example.org")).thenReturn(true);
        when(userService.fetchAllProjectManagerAdmins()).thenReturn(List.of(
                new ProjectManagerAdminUser(1L, "zoe@example.org"), new ProjectManagerAdminUser(2L, "new@example.org")));

        assertThat(dtoUserService.fetchProjectManagerAdmins())
                .extracting(de.samply.frontend.dto.User::email, de.samply.frontend.dto.User::configured)
                .containsExactly(tuple("new@example.org", false), tuple("zoe@example.org", true));
    }

    @Test
    void listsTheSiteAdminsBySiteWithTheSiteName() {
        when(bridgeheadsConfiguration.getHumanReadable("jurassic-park")).thenReturn(Optional.of("Jurassic Park"));
        when(userService.fetchAllBridgeheadAdmins()).thenReturn(List.of(
                new BridgeheadAdminUser(1L, "zoe@example.org", "jurassic-park"),
                new BridgeheadAdminUser(2L, "new@example.org", "atlantis")));

        List<de.samply.frontend.dto.User> admins = dtoUserService.fetchBridgeheadAdmins();

        assertThat(admins).extracting(de.samply.frontend.dto.User::bridgehead)
                .containsExactly("atlantis", "jurassic-park");
        assertThat(admins.get(1).humanReadableBridgehead()).isEqualTo("Jurassic Park");
    }

}
