package de.samply.modules;

import de.samply.annotations.ModuleComponent;
import de.samply.coder.CoderJob;
import de.samply.exporter.ExporterJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A class of an optional module is only created when its module is enabled. So only classes of the same module, or of a
 * module that requires it, may inject it; everything else must depend on the module's interface (real implementation or
 * stand-in). Otherwise the backend would not start with the module disabled - this finds it without starting.
 */
class ModuleDependenciesTest {

    @Test
    void onlyClassesOfTheSameOrARequiringModuleInjectModuleClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        List<Class<?>> components = scanner.findCandidateComponents("de.samply").stream()
                .map(BeanDefinition::getBeanClassName)
                .<Class<?>>map(this::loadClass)
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
    }

    private boolean mayInject(Optional<OptionalModule> componentModule, OptionalModule dependencyModule) {
        return componentModule
                .map(module -> module == dependencyModule || module.getRequiredModules().contains(dependencyModule))
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
