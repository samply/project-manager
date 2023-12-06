package de.samply.annotations;

import de.samply.frontend.Module;
import de.samply.frontend.Site;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface FrontendAction {
    Site site = null;
    Module module = null;

}
