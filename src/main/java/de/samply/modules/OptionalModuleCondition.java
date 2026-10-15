package de.samply.modules;

import de.samply.annotations.ConditionalOnModule;
import de.samply.annotations.ConditionalOnModuleDisabled;
import de.samply.annotations.ConditionalOnModuleTest;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Condition of {@link ConditionalOnModule} (matches when the module is "true"), {@link ConditionalOnModuleTest} ("test") and
 * {@link ConditionalOnModuleDisabled} ("false"). A SpringBootCondition, so the outcome also appears in Spring's condition report (start with --debug).
 */
public class OptionalModuleCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // Each annotation stands for one mode; a bean of both the real and the test mode carries two
        MergedAnnotations annotations = metadata.getAnnotations();
        List<String> outcomes = Stream.of(
                        Map.entry(ConditionalOnModule.class, ModuleMode.TRUE),
                        Map.entry(ConditionalOnModuleTest.class, ModuleMode.TEST),
                        Map.entry(ConditionalOnModuleDisabled.class, ModuleMode.FALSE))
                .filter(annotationAndMode -> annotations.isPresent(annotationAndMode.getKey()))
                .map(annotationAndMode -> {
                    OptionalModule module = annotations.get(annotationAndMode.getKey()).getEnum("value", OptionalModule.class);
                    ModuleMode mode = module.fetchMode(context.getEnvironment());
                    return (mode == annotationAndMode.getValue() ? "+" : "-") + module + " (" + module.describeSwitch()
                            + ") " + mode + ", @" + annotationAndMode.getKey().getSimpleName();
                })
                .toList();
        String message = "Optional module " + String.join("; ", outcomes);
        return outcomes.stream().anyMatch(outcome -> outcome.startsWith("+"))
                ? ConditionOutcome.match(message) : ConditionOutcome.noMatch(message);
    }

}
