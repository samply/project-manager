package de.samply.datashield;

import lombok.EqualsAndHashCode;
import lombok.Getter;


@EqualsAndHashCode(of = {"email", "projectCode"})
@Getter
public class ProjectEmail {
    private String email;
    private String projectCode;

    public ProjectEmail(String email, String projectCode) {
        this.email = email;
        this.projectCode = projectCode;
    }

}
