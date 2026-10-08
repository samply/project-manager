package de.samply.modules;

import de.samply.annotations.ModuleComponent;
import de.samply.annotations.ModuleStandIn;
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
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A class of an optional module is only created when its module is enabled. So only classes of the same module, or of a
 * module that requires it, may inject it; everything else must depend on the module's interface (real implementation or
 * stand-in). Otherwise the backend would not start with the module disabled - this finds it without starting. The real
 * implementation of such an interface must be @Primary, so that the IDE does not report two candidates.
 * <p>
 * The other way round: a class outside every module that only classes of modules inject probably belongs to a module -
 * with the module disabled it would be created for nothing and might still need its configuration.
 */
class ModuleDependenciesTest {

    @Test
    void onlyClassesOfTheSameOrARequiringModuleInjectModuleClasses() {
        // The scan evaluates the module conditions: with all modules enabled it finds the real implementations, with all
        // disabled the stand-ins
        List<Class<?>> components = Stream.of(true, false)
                .flatMap(enabled -> scanComponents(enabled).stream())
                .distinct()
                .toList();
        // The scan must find the components, module classes included, or the check below proves nothing
        assertThat(components).contains(CoderJob.class, ExporterJob.class);

        List<String> violations = components.stream()
                .flatMap(component -> Arrays.stream(component.getDeclaredConstructors())
                        .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                        .flatMap(dependency -> fetchModule(dependency)
                                .filter(module -> !mayInject(fetchModule(component), module))
                                .map(module -> component.getSimpleName() + " injects " + dependency.getSimpleName()
                                        + " of module " + module)
                                .stream()))
                .distinct()
                .toList();

        assertThat(violations).isEmpty();

        // The real implementation of an interface with a stand-in is @Primary, or IntelliJ reports two candidates
        List<Class<?>> standIns = components.stream().filter(component -> component.isAnnotationPresent(ModuleStandIn.class)).toList();
        List<String> notPrimary = components.stream()
                .filter(component -> component.isAnnotationPresent(ModuleComponent.class))
                .filter(component -> standIns.stream().anyMatch(standIn -> Arrays.stream(standIn.getInterfaces())
                        .anyMatch(standInInterface -> standInInterface.isAssignableFrom(component))))
                .filter(component -> !component.isAnnotationPresent(Primary.class))
                .map(Class::getSimpleName)
                .toList();
        assertThat(standIns).isNotEmpty();
        assertThat(notPrimary).isEmpty();
    }

    @Test
    void classesOnlyInjectedByModulesBelongToAModule() {
        List<Class<?>> components = Stream.of(true, false)
                .flatMap(enabled -> scanComponents(enabled).stream())
                .distinct()
                .toList();

        // Spring Data repositories (e.g. ProjectCoderRepository, only used by the research environment) are interfaces
        // and not scanned here; they need no configuration
        List<String> moduleOnly = components.stream()
                .filter(component -> fetchModule(component).isEmpty() && !component.isAnnotationPresent(ModuleStandIn.class))
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

    private List<Class<?>> scanComponents(boolean modulesEnabled) {
        MockEnvironment environment = new MockEnvironment();
        // An implicit module has no variable: it follows the modules that require it
        Arrays.stream(OptionalModule.values())
                .filter(module -> !module.isImplicit())
                .forEach(module -> environment.setProperty(module.getEnableVariable(), String.valueOf(modulesEnabled)));
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        return scanner.findCandidateComponents("de.samply").stream()
                .map(BeanDefinition::getBeanClassName)
                .<Class<?>>map(this::loadClass)
                .toList();
    }

    private boolean mayInject(Optional<OptionalModule> componentModule, OptionalModule dependencyModule) {
        return componentModule
                .map(module -> module == dependencyModule || module.requires(dependencyModule))
                .orElse(false);
    }

    private Optional<OptionalModule> fetchModule(Class<?> type) {
        return Optional.ofNullable(type.getAnnotation(ModuleComponent.class)).map(ModuleComponent::value);
    }

    private Class<?> loadClass(String className) {
        try {
            return ClassUtils.forName(className, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}
