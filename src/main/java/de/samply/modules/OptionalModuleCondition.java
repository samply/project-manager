package de.samply.modules;

import de.samply.annotations.ModuleComponent;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition of {@link ModuleComponent}: matches when the bean's module is enabled. A SpringBootCondition, so the outcome
 * also appears in Spring's condition report (start with --debug).
 */
public class OptionalModuleCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        OptionalModule module = metadata.getAnnotations().get(ModuleComponent.class)
                .getEnum("value", OptionalModule.class);
        String message = "Optional module " + module + " (" + module.getEnableVariable() + ")";
        return module.isEnabled(context.getEnvironment())
                ? ConditionOutcome.match(message + " enabled")
                : ConditionOutcome.noMatch(message + " disabled");
    }

}
