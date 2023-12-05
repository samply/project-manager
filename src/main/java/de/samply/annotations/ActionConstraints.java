package de.samply.annotations;

import de.samply.frontend.Module;
import de.samply.frontend.Site;
import de.samply.project.ProjectBridgeheadState;
import de.samply.project.ProjectState;
import de.samply.user.ProjectRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ActionConstraints {

    Site site = null;
    Module module = null;
    ProjectRole projectRole = null;
    ProjectState projectState = null;
    ProjectBridgeheadState projectBridgeheadState = null;

}
