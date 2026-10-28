package de.samply.app;

import de.samply.annotations.RequiresModule;
import de.samply.modules.OptionalModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectManagerControllerModulesTest {

    // The profile that was meant to disable it had no effect on a controller method; the module does
    @Test
    void executingAQueryAtABridgeheadNeedsTheExternalExecutionModule() {
        Method endpoint = Arrays.stream(ProjectManagerController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("saveAndExecuteQueryInBridgehead"))
                .findFirst().orElseThrow();

        assertThat(endpoint.getAnnotation(RequiresModule.class).value()).isEqualTo(OptionalModule.EXTERNAL_EXECUTION);
        assertThat(endpoint.getAnnotation(Profile.class)).isNull();
    }

    // The step is only offered with ENABLE_LOCAL_APPROVAL; skipping it (start final) and going back to review are not
    @Test
    void startingTheLocalApprovalNeedsTheLocalApprovalModule() {
        assertThat(endpoint("startLocalApprovalStage").getAnnotation(RequiresModule.class).value())
                .isEqualTo(OptionalModule.LOCAL_APPROVAL);
        assertThat(endpoint("startFinalStage").getAnnotation(RequiresModule.class)).isNull();
        assertThat(endpoint("requestChangesInApproval").getAnnotation(RequiresModule.class)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"uploadLocalApproval", "downloadLocalApproval", "fetchLocalApprovalDescription",
            "existsLocalApproval"})
    void theLocalApprovalDocumentNeedsTheLocalApprovalModule(String name) {
        assertThat(endpoint(name).getAnnotation(RequiresModule.class).value()).isEqualTo(OptionalModule.LOCAL_APPROVAL);
    }

    private Method endpoint(String name) {
        return Arrays.stream(ProjectManagerController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst().orElseThrow();
    }

}
