package de.samply.token;

import de.samply.user.roles.ProjectRole;
import lombok.EqualsAndHashCode;
import lombok.Getter;


@EqualsAndHashCode(of = {"email", "projectCode"})
@Getter
public class ProjectEmail {
    private String email;
    private String projectCode;
    private ProjectRole projectRole;

    public ProjectEmail(String email, String projectCode, ProjectRole projectRole) {
        this.email = email;
        this.projectCode = projectCode;
        this.projectRole = projectRole;
    }

}
