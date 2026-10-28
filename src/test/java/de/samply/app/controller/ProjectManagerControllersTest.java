package de.samply.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectManagerControllersTest {

    // A controller missing from the list would have no frontend actions and no roles per path
    @Test
    void everyControllerIsListed() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<String> controllers = scanner.findCandidateComponents("de.samply.app").stream()
                .map(BeanDefinition::getBeanClassName)
                .collect(Collectors.toSet());

        assertThat(ProjectManagerControllers.CONTROLLERS).extracting(Class::getName)
                .containsExactlyInAnyOrderElementsOf(controllers);
    }

}
