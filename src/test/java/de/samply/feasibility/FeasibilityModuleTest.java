package de.samply.feasibility;

import de.samply.annotations.ModuleComponent;
import de.samply.annotations.ModuleStandIn;
import de.samply.annotations.ModuleTest;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.modules.OptionalModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ClassUtils;
import reactor.test.StepVerifier;

import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FeasibilityModuleTest {

    @Test
    void beamImplementationBelongsToTheFeasibilityModule() {
        assertThat(BeamFeasibilityService.class.getAnnotation(ModuleComponent.class).value())
                .isEqualTo(OptionalModule.FEASIBILITY);
    }

    @Test
    void standInReplacesItWhenTheModuleIsDisabled() {
        assertThat(DisabledFeasibilityService.class.getAnnotation(ModuleStandIn.class).value())
                .isEqualTo(OptionalModule.FEASIBILITY);
    }

    @Test
    void testImplementationAndMapperBelongToTheTestMode() {
        assertThat(OptionalModule.FEASIBILITY.isWithTestMode()).isTrue();
        assertThat(TestFeasibilityService.class.getAnnotation(ModuleTest.class).value()).isEqualTo(OptionalModule.FEASIBILITY);
        assertThat(TestFeasibilityService.class.getAnnotation(ModuleComponent.class)).isNull();
        assertThat(TestFeasibilityResultResolver.class.getAnnotation(ModuleTest.class).value()).isEqualTo(OptionalModule.FEASIBILITY);
        assertThat(TestFeasibilityResultResolver.class.getAnnotation(ModuleComponent.class)).isNull();
        assertThat(FeasibilityMapper.class.getAnnotation(ModuleComponent.class).value()).isEqualTo(OptionalModule.FEASIBILITY);
        assertThat(FeasibilityMapper.class.getAnnotation(ModuleTest.class).value()).isEqualTo(OptionalModule.FEASIBILITY);
    }

    // The real classes under Spring's own condition evaluation, for each value of ENABLE_FEASIBILITY
    @ParameterizedTest
    @CsvSource({
            "true, BeamFeasibilityService, FeasibilityMapper",
            "test, TestFeasibilityService, FeasibilityMapper",
            "false, DisabledFeasibilityService,"
    })
    void eachModeCreatesItsFeasibilityService(String mode, String expectedService, String expectedMapper) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false,
                new MockEnvironment().withProperty("ENABLE_FEASIBILITY", mode));
        scanner.addIncludeFilter(new AssignableTypeFilter(FeasibilityService.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(FeasibilityMapper.class));

        assertThat(scanner.findCandidateComponents("de.samply.feasibility"))
                .extracting(candidate -> ClassUtils.getShortName(candidate.getBeanClassName()))
                .containsExactlyInAnyOrderElementsOf(Stream.of(expectedService, expectedMapper).filter(Objects::nonNull).toList());
    }

    @Test
    void standInReturnsNoResult() {
        Project project = new Project();
        project.setCode("REQ-2026-0001");
        ProjectBridgehead bridgehead = new ProjectBridgehead();
        bridgehead.setBridgehead("site-a");

        StepVerifier.create(new DisabledFeasibilityService().fetchFeasibility(project, bridgehead))
                .verifyComplete();
    }

}
