package de.samply.user;

import de.samply.db.model.CentralApprovalCommitteeUser;
import de.samply.db.repository.BridgeheadAdminUserRepository;
import de.samply.db.repository.CentralApprovalCommitteeUserRepository;
import de.samply.db.repository.CreatorUserRepository;
import de.samply.db.repository.ProjectManagerAdminUserRepository;
import de.samply.db.repository.UserRepository;
import de.samply.notification.NotificationService;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.ProjectService;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRoleToProjectRoleMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The role lists store addresses in lower case, whoever adds or removes them
class UserServiceRoleListsTest {

    private final CentralApprovalCommitteeUserRepository committeeRepository =
            mock(CentralApprovalCommitteeUserRepository.class);
    private final UserService userService = new UserService(mock(NotificationService.class),
            mock(BridgeheadAdminUserRepository.class), mock(ProjectManagerAdminUserRepository.class),
            committeeRepository, mock(UserRepository.class), mock(CreatorUserRepository.class),
            mock(SessionUser.class), mock(OrganisationRoleToProjectRoleMapper.class),
            mock(ProjectBridgeheadService.class), mock(ProjectBridgeheadUserService.class),
            mock(ProjectService.class));

    @Test
    void storesTheAddressInLowerCase() {
        when(committeeRepository.findFirstByEmail("zoe@example.org")).thenReturn(Optional.empty());

        userService.createCentralApprovalCommitteeUserIfNotExists(" Zoe@Example.org");

        ArgumentCaptor<CentralApprovalCommitteeUser> saved = ArgumentCaptor.forClass(CentralApprovalCommitteeUser.class);
        verify(committeeRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("zoe@example.org");
    }

    @Test
    void doesNotAddAnAddressWrittenDifferently() {
        when(committeeRepository.findFirstByEmail("zoe@example.org"))
                .thenReturn(Optional.of(new CentralApprovalCommitteeUser(1L, "zoe@example.org")));

        userService.createCentralApprovalCommitteeUserIfNotExists("ZOE@example.org");

        verify(committeeRepository, never()).save(any());
    }

    @Test
    void removesTheNormalizedAddress() {
        userService.removeCentralApprovalCommitteeUser("Zoe@Example.org");

        verify(committeeRepository).deleteByEmail("zoe@example.org");
    }

}
