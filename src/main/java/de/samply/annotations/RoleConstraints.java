package de.samply.annotations;

import de.samply.user.OrganisationRole;
import de.samply.user.ProjectRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RoleConstraints {

    OrganisationRole[] organisationRoles = null;
    ProjectRole[] projectRoles = null;

}
