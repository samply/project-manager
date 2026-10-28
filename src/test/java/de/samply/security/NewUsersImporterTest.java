package de.samply.security;

import de.samply.user.UserService;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.UserOrganisationRoles;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// At login, project manager admins and committee members are stored, so that they get emails
class NewUsersImporterTest {

    private final UserService userService = mock(UserService.class);

    @Test
    void storesACommitteeMember() {
        importUserWithRole(OrganisationRole.CENTRAL_APPROVAL_COMMITTEE);

        verify(userService).createCentralApprovalCommitteeUserIfNotExists("viewer@example.org");
        verify(userService, never()).createProjectManagerAdminUserIfNotExists(any());
    }

    @Test
    void doesNotStoreAResearcherAsCommitteeMember() {
        importUserWithRole(OrganisationRole.RESEARCHER);

        verify(userService, never()).createCentralApprovalCommitteeUserIfNotExists(any());
    }

    private void importUserWithRole(OrganisationRole role) {
        SessionUser sessionUser = new SessionUser(true);
        sessionUser.setEmail("viewer@example.org");
        UserOrganisationRoles roles = new UserOrganisationRoles();
        roles.addRoleNotDependentOnBridgehead(role);
        sessionUser.setUserOrganisationRoles(roles);

        new NewUsersImporter(sessionUser, userService).importNewUsers();
    }

}
