package de.samply.datashield;

import de.samply.annotations.ModuleComponent;
import de.samply.annotations.ModuleStandIn;
import de.samply.datashield.dto.DataShieldProjectStatus;
import de.samply.datashield.dto.DataShieldTokenStatus;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.modules.OptionalModule;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DataShieldModuleTest {

    private final DisabledDataShieldService standIn = new DisabledDataShieldService();

    @Test
    void tokenManagerServiceAndJobBelongToTheDataShieldModule() {
        Stream.of(DataShieldTokenManagerService.class, DataShieldTokenManagerJob.class)
                .forEach(type -> assertThat(type.getAnnotation(ModuleComponent.class).value())
                        .as(type.getSimpleName()).isEqualTo(OptionalModule.DATASHIELD));
        assertThat(DisabledDataShieldService.class.getAnnotation(ModuleStandIn.class).value())
                .isEqualTo(OptionalModule.DATASHIELD);
    }

    @Test
    void dataShieldRequiresTheResearchEnvironment() {
        assertThat(OptionalModule.DATASHIELD.getRequiredModules()).containsExactly(OptionalModule.RESEARCH_ENVIRONMENT);
    }

    @Test
    void standInAnswersInactiveAndHasNoScript() {
        Project project = new Project();
        project.setCode("REQ-2026-0001");
        ProjectBridgehead bridgehead = new ProjectBridgehead();
        bridgehead.setBridgehead("site-a");

        StepVerifier.create(standIn.fetchProjectStatus(project, bridgehead))
                .assertNext(status -> assertThat(status.projectStatus()).isEqualTo(DataShieldProjectStatus.INACTIVE))
                .verifyComplete();
        assertThat(standIn.existsAuthenticationScript(project, bridgehead)).isFalse();
        StepVerifier.create(standIn.fetchTokenStatus(project, bridgehead, "user@example.org"))
                .assertNext(status -> assertThat(status.tokenStatus()).isEqualTo(DataShieldTokenStatus.INACTIVE))
                .verifyComplete();
        assertThat(standIn.fetchProjectBridgeheads(project, bridgehead, "user@example.org")).isEmpty();
    }

    @Test
    void standInRejectsTheTokenOperationsOfTheJob() {
        Project project = new Project();
        ProjectBridgehead bridgehead = new ProjectBridgehead();

        StepVerifier.create(standIn.removeProjectAndTokens(project, bridgehead))
                .expectError(IllegalStateException.class)
                .verify();
    }

}
