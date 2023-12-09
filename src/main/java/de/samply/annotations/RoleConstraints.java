package de.samply.annotations;

import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RoleConstraints {

    /*
     Organisation Roles are checked in SecurityConfiguration. At this level, project name is not available yet,
     because the http request between the client and the application is not the same as the http request between the
     application and the OIDC Server.
     */
    OrganisationRole[] organisationRoles() default {};

    // Project Role are checked in ProjectRoleConstrainstAspects
    ProjectRole[] projectRoles() default {};


}
