package de.samply.frontend;

import de.samply.app.controller.ProjectManagerControllers;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendServiceRequiresBridgeheadTest {

    private static Method controllerMethod(String name) {
        return ProjectManagerControllers.fetchDeclaredMethods()
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void projectLevelActionsDoNotRequireASite() {
        assertThat(FrontendService.requiresBridgehead(controllerMethod("fetchProject"))).isFalse();
        assertThat(FrontendService.requiresBridgehead(controllerMethod("existsDescription"))).isFalse();
    }

    @Test
    void siteActionsRequireASite() {
        assertThat(FrontendService.requiresBridgehead(controllerMethod("existsVotum"))).isTrue();
        assertThat(FrontendService.requiresBridgehead(controllerMethod("fetchUsersForAutocomplete"))).isTrue();
    }

    @Test
    void actionsWithoutSiteParameterDoNotRequireASite() {
        assertThat(FrontendService.requiresBridgehead(controllerMethod("fetchProjects"))).isFalse();
    }
}
