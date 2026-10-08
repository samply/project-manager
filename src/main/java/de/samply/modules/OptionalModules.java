package de.samply.modules;

import de.samply.annotations.ConditionalOnModule;
import de.samply.annotations.ConditionalOnModuleTest;
import de.samply.project.ProjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Which optional modules are enabled in this deployment. Stops the start when an enabled module requires a disabled
 * one, and logs the enabled modules with their beans once the application has started.
 */
@Slf4j
@Component
public class OptionalModules {

    private final Map<OptionalModule, ModuleMode> modes;
    private final Set<OptionalModule> enabledModules;
    private final ApplicationContext applicationContext;

    public OptionalModules(Environment environment, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.modes = Arrays.stream(OptionalModule.values())
                .collect(Collectors.toMap(module -> module, module -> module.fetchMode(environment),
                        (first, _) -> first, () -> new EnumMap<>(OptionalModule.class)));
        this.enabledModules = modes.keySet().stream()
                .filter(module -> modes.get(module) != ModuleMode.FALSE)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(OptionalModule.class)));
        checkRequiredModules();
    }

    /** Enabled: "true" or "test". */
    public boolean isEnabled(OptionalModule module) {
        return enabledModules.contains(module);
    }

    public ModuleMode fetchMode(OptionalModule module) {
        return modes.get(module);
    }

    /** A request type may be offered and used when all the modules it needs are enabled ("true" or "test"). */
    public boolean isAvailable(ProjectType projectType) {
        return enabledModules.containsAll(projectType.getRequiredModules());
    }

    public List<ProjectType> fetchAvailableProjectTypes() {
        return Arrays.stream(ProjectType.values()).filter(this::isAvailable).toList();
    }

    /** Why a request type is not available, e.g. "Request type DATASHIELD requires module DATASHIELD (ENABLE_DATASHIELD=false)". */
    public String describeUnavailable(ProjectType projectType) {
        return "Request type " + projectType + " requires " + projectType.getRequiredModules().stream()
                .filter(module -> !enabledModules.contains(module))
                .map(module -> "module " + module + " (" + module.describeSwitch()
                        + (module.isImplicit() ? "" : "=" + modes.get(module)) + ")")
                .collect(Collectors.joining(", "));
    }

    // Only for modules in mode "true": the test mode replaces the systems the module needs
    private void checkRequiredModules() {
        List<String> problems = enabledModules.stream()
                .filter(module -> modes.get(module) == ModuleMode.TRUE)
                .flatMap(module -> module.getRequiredModules().stream()
                                // An implicit module is enabled with the modules that require it, so it never appears here
                        .filter(required -> modes.get(required) != ModuleMode.TRUE)
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
        Map<OptionalModule, Set<String>> beansByModule = Stream.concat(
                        fetchBeansByModule(ConditionalOnModule.class, ConditionalOnModule::value),
                        fetchBeansByModule(ConditionalOnModuleTest.class, ConditionalOnModuleTest::value))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toCollection(TreeSet::new))));
        log.info("Enabled optional modules: {}", enabledModules.isEmpty() ? "none" : enabledModules.stream()
                .map(module -> module + (modes.get(module) == ModuleMode.TEST ? " [test]" : "")
                        + Optional.ofNullable(beansByModule.get(module))
                        .map(beans -> " (" + String.join(", ", beans) + ")")
                        .orElse("") + describeImplicitSwitch(module))
                .collect(Collectors.joining(", ")));
        log.debug("Disabled optional modules: {}", Arrays.stream(OptionalModule.values())
                .filter(module -> !enabledModules.contains(module)).toList());
    }

    // An implicit module (no variable) is enabled through the modules in mode "true" that require it
    private String describeImplicitSwitch(OptionalModule module) {
        return module.isImplicit() ? " [through " + enabledModules.stream()
                .filter(enabledModule -> modes.get(enabledModule) == ModuleMode.TRUE)
                .filter(enabledModule -> enabledModule.getRequiredModules().contains(module))
                .map(OptionalModule::name)
                .collect(Collectors.joining(", ")) + "]" : "";
    }

    private <A extends Annotation> Stream<Map.Entry<OptionalModule, String>> fetchBeansByModule(
            Class<A> annotationType, Function<A, OptionalModule> module) {
        return Arrays.stream(applicationContext.getBeanNamesForAnnotation(annotationType))
                .map(beanName -> Map.entry(module.apply(applicationContext.findAnnotationOnBean(beanName, annotationType)),
                        fetchBeanClassName(beanName)));
    }

    // The user class, not the proxy Spring may create around it (e.g. for @Async)
    private String fetchBeanClassName(String beanName) {
        return Optional.ofNullable(applicationContext.getType(beanName))
                .map(type -> ClassUtils.getUserClass(type).getSimpleName())
                .orElse(beanName);
    }

}
