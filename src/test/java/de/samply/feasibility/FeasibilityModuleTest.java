package de.samply.feasibility;

import de.samply.annotations.ModuleComponent;
import de.samply.annotations.ModuleStandIn;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.modules.OptionalModule;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

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
    void standInReturnsNoResult() {
        Project project = new Project();
        project.setCode("REQ-2026-0001");
        ProjectBridgehead bridgehead = new ProjectBridgehead();
        bridgehead.setBridgehead("site-a");

        StepVerifier.create(new DisabledFeasibilityService().fetchFeasibility(project, bridgehead))
                .verifyComplete();
    }

}
