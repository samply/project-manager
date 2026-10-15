package de.samply.annotations;

import de.samply.project.ProjectType;
import de.samply.query.QueryFormat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ProjectConstraints {
    /** When set, a document-id request parameter must belong to the current user or a project-manager admin. */
    boolean documentCreatorOrProjectManagerAdmin() default false;
    ProjectType[] projectTypes() default {};

    QueryFormat[] queryFormats() default {};
}
