package de.samply.email;

import de.samply.user.roles.ProjectRole;
import lombok.Getter;

import java.util.Optional;

@Getter
public class EmailRecipient {

    private String email;
    private Optional<String> projectCode;
    private Optional<String> bridgehead;
    private ProjectRole role;
    private Optional<String> message = Optional.empty();

    public EmailRecipient(String email, Optional<String> projectCode, Optional<String> bridgehead, ProjectRole role) {
        this.email = email;
        this.projectCode = projectCode;
        this.bridgehead = bridgehead;
        this.role = role;
    }

    public void setMessage(Optional<String> message) {
        this.message = message;
    }
    
}
