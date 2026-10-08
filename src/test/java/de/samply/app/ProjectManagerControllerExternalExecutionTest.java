package de.samply.app;

import de.samply.annotations.RequiresModule;
import de.samply.modules.OptionalModule;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectManagerControllerExternalExecutionTest {

    // The profile that was meant to disable it had no effect on a controller method; the module does
    @Test
    void executingAQueryAtABridgeheadNeedsTheExternalExecutionModule() {
        Method endpoint = Arrays.stream(ProjectManagerController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("saveAndExecuteQueryInBridgehead"))
                .findFirst().orElseThrow();

        assertThat(endpoint.getAnnotation(RequiresModule.class).value()).isEqualTo(OptionalModule.EXTERNAL_EXECUTION);
        assertThat(endpoint.getAnnotation(Profile.class)).isNull();
    }

}
