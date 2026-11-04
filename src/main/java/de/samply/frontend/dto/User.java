package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.project.state.UserProjectState;
import de.samply.user.roles.ProjectRole;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record User(
        String email,
        String firstName,
        String lastName,
        String bridgehead,
        String humanReadableBridgehead,
        ProjectRole projectRole,
        UserProjectState projectState,
        // Only in the email lists of the roles (project manager admins, committee members, site admins; see
        // docs/user-lists.md): true = the address is set in the configuration (PROJECT_MANAGER_ADMIN_EMAILS,
        // CENTRAL_APPROVAL_COMMITTEE_EMAILS, BRIDGEHEADS_CONFIG_<SITE>_ADMINEMAILS), is loaded again at every start and
        // therefore cannot be removed in the configuration page, only in the configuration; false = added at login or
        // by hand, removable. null (not sent) everywhere else
        Boolean configured
) {
}
