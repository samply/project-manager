package de.samply.researchenvironment;

import de.samply.annotations.ConditionalOnModule;
import de.samply.annotations.ConditionalOnModuleDisabled;
import de.samply.coder.CoderConfiguration;
import de.samply.coder.CoderJob;
import de.samply.coder.CoderResearchEnvironmentService;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.ProjectCoder;
import de.samply.modules.OptionalModule;
import de.samply.register.AppRegisterService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResearchEnvironmentModuleTest {

    private final DisabledResearchEnvironmentService standIn = new DisabledResearchEnvironmentService();

    @Test
    void coderAppRegisterJobAndConfigurationBelongToTheResearchEnvironmentModule() {
        Stream.of(CoderResearchEnvironmentService.class, AppRegisterService.class, CoderJob.class, CoderConfiguration.class)
                .forEach(type -> assertThat(type.getAnnotation(ConditionalOnModule.class).value())
                        .as(type.getSimpleName()).isEqualTo(OptionalModule.RESEARCH_ENVIRONMENT));
        assertThat(DisabledResearchEnvironmentService.class.getAnnotation(ConditionalOnModuleDisabled.class).value())
                .isEqualTo(OptionalModule.RESEARCH_ENVIRONMENT);
    }

    @Test
    void standInHasNoWorkspacesAndNoUrl() {
        ProjectBridgeheadUser user = new ProjectBridgeheadUser();
        user.setEmail("researcher@example.org");

        StepVerifier.create(standIn.createWorkspace(user)).verifyComplete();
        StepVerifier.create(standIn.deleteWorkspace(user)).verifyComplete();
        StepVerifier.create(standIn.deleteAllWorkspaces("REQ-2026-0001", "site-a")).verifyComplete();
        assertThat(standIn.existsUserResearchEnvironmentWorkspace(user)).isFalse();
        assertThat(standIn.fetchCoderOrderedByCreatedAtDesc("REQ-2026-0001", "site-a", "researcher@example.org")).isEmpty();
        assertThat(standIn.fetchResearchEnvironmentUrl()).isEmpty();
    }

    @Test
    void standInRejectsWhatCannotHappenWithoutTheModule() {
        assertThatThrownBy(() -> standIn.saveCoder(new ProjectCoder())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> standIn.fetchFileBeamId(new ProjectCoder())).isInstanceOf(IllegalStateException.class);
    }

}
