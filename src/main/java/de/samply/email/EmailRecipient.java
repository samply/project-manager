package de.samply.email;

import de.samply.user.roles.ProjectRole;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
public class EmailRecipient {

    private final String email;
    private final Optional<String> projectCode;
    private final Optional<String> bridgehead;
    private final ProjectRole role;
    @Setter
    private Optional<String> message = Optional.empty();

    public EmailRecipient(String email, Optional<String> projectCode, Optional<String> bridgehead, ProjectRole role) {
        this.email = email;
        this.projectCode = projectCode;
        this.bridgehead = bridgehead;
        this.role = role;
    }

}
