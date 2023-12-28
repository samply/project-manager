package de.samply.notification.smtp;

import de.samply.user.roles.ProjectRole;

import java.util.Optional;

public record EmailRecipient(String email, Optional<String> bridgehead, ProjectRole role) {
}
