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
 * Marks a bean (component class or {@code @Bean} method) as part of an optional module: Spring only creates it when
 * the module is enabled, and the start-up log lists it under its module. See docs/optional-modules.md.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(OptionalModuleCondition.class)
public @interface ConditionalOnModule {

    OptionalModule value();

}
