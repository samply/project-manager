package de.samply.modules;

import de.samply.annotations.ModuleComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which optional modules are enabled in this deployment. Stops the start when an enabled module requires a disabled
 * one, and logs the enabled modules with their beans once the application has started.
 */
@Slf4j
@Component
public class OptionalModules {

    private final Set<OptionalModule> enabledModules;
    private final ApplicationContext applicationContext;

    public OptionalModules(Environment environment, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.enabledModules = Arrays.stream(OptionalModule.values())
                .filter(module -> module.isEnabled(environment))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(OptionalModule.class)));
        checkRequiredModules();
    }

    public boolean isEnabled(OptionalModule module) {
        return enabledModules.contains(module);
    }

    private void checkRequiredModules() {
        List<String> problems = enabledModules.stream()
                .flatMap(module -> module.getRequiredModules().stream()
                        .filter(required -> !enabledModules.contains(required))
                                // An implicit module is enabled with the modules that require it, so it never appears here
                        .map(required -> module + " (" + module.describeSwitch() + ") requires " + required
                                + " (" + required.describeSwitch() + ")"))
                .toList();
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Optional modules enabled without the modules they require: "
                    + String.join("; ", problems));
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logEnabledModules() {
        Map<OptionalModule, List<String>> beansByModule = Arrays.stream(applicationContext.getBeanNamesForAnnotation(ModuleComponent.class))
                .collect(Collectors.groupingBy(
                        beanName -> applicationContext.findAnnotationOnBean(beanName, ModuleComponent.class).value(),
                        Collectors.mapping(this::fetchBeanClassName, Collectors.toList())));
        log.info("Enabled optional modules: {}", enabledModules.isEmpty() ? "none" : enabledModules.stream()
                .map(module -> module + Optional.ofNullable(beansByModule.get(module))
                        .map(beans -> " (" + String.join(", ", beans.stream().sorted().toList()) + ")")
                        .orElse("") + describeImplicitSwitch(module))
                .collect(Collectors.joining(", ")));
        log.debug("Disabled optional modules: {}", Arrays.stream(OptionalModule.values())
                .filter(module -> !enabledModules.contains(module)).toList());
    }

    // An implicit module (no variable) is enabled through the enabled modules that require it
    private String describeImplicitSwitch(OptionalModule module) {
        return module.isImplicit() ? " [through " + enabledModules.stream()
                .filter(enabledModule -> enabledModule.getRequiredModules().contains(module))
                .map(OptionalModule::name)
                .collect(Collectors.joining(", ")) + "]" : "";
    }

    // The user class, not the proxy Spring may create around it (e.g. for @Async)
    private String fetchBeanClassName(String beanName) {
        return Optional.ofNullable(applicationContext.getType(beanName))
                .map(type -> ClassUtils.getUserClass(type).getSimpleName())
                .orElse(beanName);
    }

}
