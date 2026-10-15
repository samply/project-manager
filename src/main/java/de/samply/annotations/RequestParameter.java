package de.samply.annotations;

import de.samply.app.ProjectManagerConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER) // Applies to method parameters
@Retention(RetentionPolicy.RUNTIME) // Retain at runtime for reflection
public @interface RequestParameter {
    String name();

    boolean required() default true;

    boolean notEmpty() default false;

    String defaultValue() default ProjectManagerConst.NOT_SET;
}
