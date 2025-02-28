package de.samply.security;

import de.samply.user.UserService;
import de.samply.user.roles.OrganisationRole;
import org.springframework.stereotype.Component;

@Component
public class NewUsersImporter {

    private final SessionUser sessionUser;
    private final UserService userService;

    public NewUsersImporter(SessionUser sessionUser, UserService userService) {
        this.sessionUser = sessionUser;
        this.userService = userService;
    }

    public void importNewUsers() {
        if (sessionUser.getUserOrganisationRoles().getRolesNotDependentOnBridgeheads().contains(OrganisationRole.PROJECT_MANAGER_ADMIN)) {
            userService.createProjectManagerAdminUserIfNotExists(sessionUser.getEmail());
        }
        sessionUser.getBridgeheads().forEach(bridgehead -> {
            if (sessionUser.getUserOrganisationRoles().getBridgeheadRoles(bridgehead).contains(OrganisationRole.BRIDGEHEAD_ADMIN)) {
                userService.createBridgeheadAdminUserIfNotExists(sessionUser.getEmail(), bridgehead);
            }
        });
        userService.addUserInformationIfNotExists(sessionUser.getEmail(), sessionUser.getFirstName(), sessionUser.getLastName());
    }

}
