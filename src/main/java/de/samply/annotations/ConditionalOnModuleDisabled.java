package de.samply.annotations;

import de.samply.modules.OptionalModule;
import de.samply.modules.OptionalModuleCondition;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the disabled implementation of an optional module: the implementation of the module's interface that Spring
 * creates only when
 * the module is <b>disabled</b>, in place of the real one ({@link ConditionalOnModule}). See docs/optional-modules.md.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(OptionalModuleCondition.class)
public @interface ConditionalOnModuleDisabled {

    OptionalModule value();

}
