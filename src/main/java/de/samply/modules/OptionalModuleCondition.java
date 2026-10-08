package de.samply.modules;

import de.samply.annotations.ModuleComponent;
import de.samply.annotations.ModuleStandIn;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition of {@link ModuleComponent} (matches when the module is enabled) and {@link ModuleStandIn} (matches when it
 * is disabled). A SpringBootCondition, so the outcome also appears in Spring's condition report (start with --debug).
 */
public class OptionalModuleCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        MergedAnnotations annotations = metadata.getAnnotations();
        boolean standIn = annotations.isPresent(ModuleStandIn.class);
        OptionalModule module = (standIn ? annotations.get(ModuleStandIn.class) : annotations.get(ModuleComponent.class))
                .getEnum("value", OptionalModule.class);
        boolean enabled = module.isEnabled(context.getEnvironment());
        String message = "Optional module " + module + " (" + module.describeSwitch() + ") "
                + (enabled ? "enabled" : "disabled") + (standIn ? ": stand-in" : "");
        return enabled != standIn ? ConditionOutcome.match(message) : ConditionOutcome.noMatch(message);
    }

}
