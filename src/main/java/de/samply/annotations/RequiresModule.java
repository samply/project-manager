package de.samply.annotations;

import de.samply.modules.OptionalModule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An endpoint that only exists for an optional module: when the module is disabled, its action is not offered to the
 * frontend and a call is refused (405), like the other constraints. For endpoints without a bean of the module behind
 * them (a disabled module's service would otherwise answer through its disabled implementation). See
 * docs/optional-modules.md.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresModule {

    OptionalModule value();

}
