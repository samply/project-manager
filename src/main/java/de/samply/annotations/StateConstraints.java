package de.samply.annotations;

import de.samply.project.ProjectBridgeheadState;
import de.samply.project.ProjectState;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface StateConstraints {
    ProjectState[] projectStates = null;
    ProjectBridgeheadState[] projectBridgeheadStates = null;

}
