package de.samply.email;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.user.roles.ProjectRole;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
public class EmailRecipient {

    private final String email;
    private final Optional<Project> project;
    private final Optional<ProjectBridgehead> bridgehead;
    private final ProjectRole role;
    @Setter
    private Optional<String> message = Optional.empty();

    public EmailRecipient(String email, Optional<Project> project, Optional<ProjectBridgehead> bridgehead, ProjectRole role) {
        this.email = email;
        this.project = project;
        this.bridgehead = bridgehead;
        this.role = role;
    }

}
