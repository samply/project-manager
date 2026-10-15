package de.samply.app;

import de.samply.annotations.FrontendAction;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectManagerControllerFormTitleOrderTest {

    @Test
    void exposesConfiguredFormTitleOrderForTheFrontend() throws Exception {
        Method endpoint = ProjectManagerController.class.getDeclaredMethod(
                "fetchProjectFormTitleCanonicalOrder",
                Project.class, ProjectBridgehead.class, String.class);

        assertThat(endpoint.getAnnotation(GetMapping.class).value())
                .containsExactly("/project/forms/title/order");
        assertThat(endpoint.getAnnotation(FrontendAction.class).action())
                .isEqualTo("FETCH_PROJECT_FORM_TITLE_ORDER");
    }
}
