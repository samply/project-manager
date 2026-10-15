package de.samply.frontend;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class FrontendServiceActionPackagesByBridgeheadTest {

    private static ProjectBridgehead bridgehead(String id) {
        ProjectBridgehead bridgehead = new ProjectBridgehead();
        bridgehead.setBridgehead(id);
        return bridgehead;
    }

    // The entity's equality does not tell bridgeheads apart by their id
    private static Optional<ProjectBridgehead> atBridgehead(String id) {
        return argThat(bridgehead -> bridgehead != null && bridgehead.isPresent()
                && id.equals(bridgehead.get().getBridgehead()));
    }

    @Test
    void onePackageWithoutBridgeheadAndOnePerBridgeheadInOrder() {
        FrontendService service = spy(new FrontendService(null, null, null, null, null, null, "en"));
        Project project = mock(Project.class);
        ProjectBridgehead tum = bridgehead("tum");
        ProjectBridgehead lmu = bridgehead("lmu");
        Map<String, Map<String, Action>> withoutBridgehead = Map.of("PROJECTS", Map.of());
        Map<String, Map<String, Action>> atTum = Map.of("TUM", Map.of());
        Map<String, Map<String, Action>> atLmu = Map.of("LMU", Map.of());
        doReturn(withoutBridgehead).when(service).fetchModuleActionPackage(eq("project-view"), eq(Optional.of(project)),
                eq(Optional.empty()), any(), eq(true));
        doReturn(atTum).when(service).fetchModuleActionPackage(eq("project-view"), eq(Optional.of(project)),
                atBridgehead("tum"), any(), eq(true));
        doReturn(atLmu).when(service).fetchModuleActionPackage(eq("project-view"), eq(Optional.of(project)),
                atBridgehead("lmu"), any(), eq(true));

        Map<String, Map<String, Map<String, Action>>> result =
                service.fetchModuleActionPackagesByBridgehead("project-view", Optional.of(project),
                        Arrays.asList(tum, null, lmu, tum), Optional.empty());

        assertThat(result.keySet()).containsExactly("", "tum", "lmu");
        assertThat(result.get("")).isSameAs(withoutBridgehead);
        assertThat(result.get("tum")).isSameAs(atTum);
        assertThat(result.get("lmu")).isSameAs(atLmu);
    }
}
