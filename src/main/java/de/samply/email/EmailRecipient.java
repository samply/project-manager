package de.samply.email;

import de.samply.user.roles.ProjectRole;

import java.util.Optional;

public record EmailRecipient(String email, Optional<String> projectCode, Optional<String> bridgehead, ProjectRole role) {
}
