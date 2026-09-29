package de.samply.frontend;

import de.samply.app.ProjectManagerController;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendServiceEmailRecipientsTest {

    private static Method controllerMethod(String name) {
        return Arrays.stream(ProjectManagerController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void recipientsOfEveryEmailSenderAreListedOnce() {
        assertThat(FrontendService.fetchEmailRecipients(controllerMethod("acceptProject")))
                .containsExactlyInAnyOrder("CREATOR", "ALL_BRIDGEHEAD_ADMINS");
    }

    @Test
    void actionsWithoutEmailSenderHaveNoRecipients() {
        assertThat(FrontendService.fetchEmailRecipients(controllerMethod("fetchProject"))).isEmpty();
    }
}
