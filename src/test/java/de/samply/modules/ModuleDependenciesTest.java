package de.samply.modules;

import de.samply.annotations.ConditionalOnModule;
import de.samply.annotations.ConditionalOnModuleDisabled;
import de.samply.annotations.ConditionalOnModuleTest;
import de.samply.coder.CoderJob;
import de.samply.exporter.ExporterJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A class of an optional module is only created when its module is enabled. So only classes of the same module, or of a
 * module that requires it, may inject it; everything else must depend on the module's interface (real implementation or
 * disabled implementation). Otherwise the backend would not start with the module disabled - this finds it without starting. The real
 * implementation of such an interface must be @Primary, so that the IDE does not report two candidates.
 * <p>
 * The other way round: a class outside every module that only classes of modules inject probably belongs to a module -
 * with the module disabled it would be created for nothing and might still need its configuration.
 */
class ModuleDependenciesTest {

    @Test
    void onlyClassesOfTheSameOrARequiringModuleInjectModuleClasses() {
        // The scan evaluates the module conditions: with all modules "true" it finds the real implementations, with all
        // "false" the disabled implementations, with "test" the test implementations
        List<Class<?>> components = scanAllComponents();
        // The scan must find the components, module classes included, or the check below proves nothing
        assertThat(components).contains(CoderJob.class, ExporterJob.class);

        List<String> violations = components.stream()
                .flatMap(component -> Arrays.stream(component.getDeclaredConstructors())
                        .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                        .flatMap(dependency -> fetchModule(dependency)
                                .filter(module -> !mayInject(component, dependency))
                                .map(module -> component.getSimpleName() + " " + fetchModes(component) + " injects "
                                        + dependency.getSimpleName() + " " + fetchModes(dependency) + " of module " + module)
                                .stream()))
                .distinct()
                .toList();

        assertThat(violations).isEmpty();

        // An interface with several module implementations (real, test, disabled) needs exactly one @Primary, or
        // IntelliJ reports several candidates - at runtime only one of them exists (module conditions)
        List<Class<?>> moduleImplementations = components.stream()
                .filter(component -> !fetchModes(component).isEmpty() || component.isAnnotationPresent(ConditionalOnModuleDisabled.class))
                .toList();
        List<String> withoutOnePrimary = moduleImplementations.stream()
                .flatMap(component -> Arrays.stream(component.getInterfaces()))
                .distinct()
                .filter(moduleInterface -> moduleImplementations.stream().filter(moduleInterface::isAssignableFrom).count() > 1)
                .filter(moduleInterface -> moduleImplementations.stream()
                        .filter(moduleInterface::isAssignableFrom)
                        .filter(component -> component.isAnnotationPresent(Primary.class))
                        .count() != 1)
                .map(Class::getSimpleName)
                .toList();
        assertThat(moduleImplementations).isNotEmpty();
        assertThat(withoutOnePrimary).isEmpty();
    }

    @Test
    void classesOnlyInjectedByModulesBelongToAModule() {
        List<Class<?>> components = scanAllComponents();

        // Spring Data repositories (e.g. ProjectCoderRepository, only used by the research environment) are interfaces
        // and not scanned here; they need no configuration
        List<String> moduleOnly = components.stream()
                .filter(component -> fetchModule(component).isEmpty() && !component.isAnnotationPresent(ConditionalOnModuleDisabled.class))
                .flatMap(component -> {
                    List<Class<?>> injectors = fetchInjectors(component, components);
                    return injectors.isEmpty() || injectors.stream().anyMatch(injector -> fetchModule(injector).isEmpty())
                            ? Stream.empty()
                            : Stream.of(component.getSimpleName() + " is only injected by modules "
                            + injectors.stream().map(injector -> fetchModule(injector).orElseThrow()).distinct().sorted().toList()
                            + " - does it belong to one of them?");
                })
                .toList();

        assertThat(moduleOnly).isEmpty();
    }

    private List<Class<?>> fetchInjectors(Class<?> component, List<Class<?>> components) {
        return components.stream()
                .filter(injector -> injector != component)
                .filter(injector -> Arrays.stream(injector.getDeclaredConstructors())
                        .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                        .anyMatch(dependency -> dependency != Object.class && dependency.isAssignableFrom(component)))
                .toList();
    }

    private List<Class<?>> scanAllComponents() {
        return Stream.of(ModuleMode.TRUE, ModuleMode.FALSE, ModuleMode.TEST)
                .flatMap(mode -> scanComponents(mode).stream())
                .distinct()
                .toList();
    }

    // All modules in the given mode; for "test", the modules without a test mode stay "true"
    private List<Class<?>> scanComponents(ModuleMode mode) {
        MockEnvironment environment = new MockEnvironment();
        // An implicit module has no variable: it follows the modules that require it
        Arrays.stream(OptionalModule.values())
                .filter(module -> !module.isImplicit())
                .forEach(module -> environment.setProperty(module.getEnableVariable(),
                        (mode == ModuleMode.TEST && !module.isWithTestMode() ? ModuleMode.TRUE : mode).toString()));
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        return scanner.findCandidateComponents("de.samply").stream()
                .map(BeanDefinition::getBeanClassName)
                .<Class<?>>map(this::loadClass)
                .toList();
    }

    // The dependency must exist whenever the component exists: in the same module, in each of the component's modes; in
    // another module, the component must exist only in mode "true" (requirements only hold then) and require it
    private boolean mayInject(Class<?> component, Class<?> dependency) {
        Optional<OptionalModule> componentModule = fetchModule(component);
        OptionalModule dependencyModule = fetchModule(dependency).orElseThrow();
        Set<ModuleMode> componentModes = fetchModes(component);
        Set<ModuleMode> dependencyModes = fetchModes(dependency);
        return componentModule
                .map(module -> module == dependencyModule
                        ? dependencyModes.containsAll(componentModes)
                        : componentModes.equals(Set.of(ModuleMode.TRUE)) && module.requires(dependencyModule)
                        && dependencyModes.contains(ModuleMode.TRUE))
                .orElse(false);
    }

    // The module of a class of its real mode (@ConditionalOnModule) or of its test mode (@ConditionalOnModuleTest)
    private Optional<OptionalModule> fetchModule(Class<?> type) {
        return Optional.ofNullable(type.getAnnotation(ConditionalOnModule.class)).map(ConditionalOnModule::value)
                .or(() -> Optional.ofNullable(type.getAnnotation(ConditionalOnModuleTest.class)).map(ConditionalOnModuleTest::value));
    }

    private Set<ModuleMode> fetchModes(Class<?> type) {
        return Stream.of(type.isAnnotationPresent(ConditionalOnModule.class) ? ModuleMode.TRUE : null,
                        type.isAnnotationPresent(ConditionalOnModuleTest.class) ? ModuleMode.TEST : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Class<?> loadClass(String className) {
        try {
            return ClassUtils.forName(className, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}
