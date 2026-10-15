package de.samply.app;

import de.samply.annotations.FrontendAction;
import de.samply.annotations.RoleConstraints;
import de.samply.annotations.StateConstraints;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectManagerControllerFormLayoutsTest {

    @Test
    void exposesLayoutsWithTheSameRoleAndStateRestrictionsAsFormFields() throws Exception {
        Method formFieldsEndpoint = ProjectManagerController.class.getDeclaredMethod(
                "fetchProjectFormFields",
                Project.class, ProjectBridgehead.class, String.class, String.class);
        Method formLayoutsEndpoint = ProjectManagerController.class.getDeclaredMethod(
                "fetchProjectFormLayouts",
                Project.class, ProjectBridgehead.class, String.class);

        assertThat(formLayoutsEndpoint.getAnnotation(RoleConstraints.class).projectRoles())
                .containsExactly(formFieldsEndpoint.getAnnotation(RoleConstraints.class).projectRoles());
        assertThat(formLayoutsEndpoint.getAnnotation(StateConstraints.class))
                .isEqualTo(formFieldsEndpoint.getAnnotation(StateConstraints.class));
        assertThat(formLayoutsEndpoint.getAnnotation(GetMapping.class).value())
                .containsExactly(ProjectManagerConst.FETCH_PROJECT_FORM_LAYOUTS);
        assertThat(formLayoutsEndpoint.getAnnotation(FrontendAction.class).action())
                .isEqualTo(ProjectManagerConst.FETCH_PROJECT_FORM_LAYOUTS_ACTION);
    }
}
