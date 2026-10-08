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
 * Marks a bean of the <b>test</b> mode of an optional module (its variable is "test"): created in place of the real
 * beans ({@link ConditionalOnModule}), e.g. an implementation with random results that needs no external system. A bean
 * needed in both modes (e.g. a mapper) carries both annotations. See docs/optional-modules.md.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(OptionalModuleCondition.class)
public @interface ConditionalOnModuleTest {

    OptionalModule value();

}
