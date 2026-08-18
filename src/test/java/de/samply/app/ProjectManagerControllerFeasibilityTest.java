package de.samply.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.annotations.FrontendAction;
import de.samply.annotations.ProjectConstraints;
import de.samply.annotations.RoleConstraints;
import de.samply.annotations.StateConstraints;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.feasibility.FeasibilityService;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryFormat;
import de.samply.user.roles.OrganisationRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectManagerControllerFeasibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FeasibilityService feasibilityService;

    @InjectMocks
    private ProjectManagerController controller;

    @Test
    void exposesFeasibilityWithRequiredConstraints() throws Exception {
        Method endpoint = ProjectManagerController.class.getDeclaredMethod(
                "fetchFeasibility", Project.class, ProjectBridgehead.class);

        assertThat(endpoint.getAnnotation(GetMapping.class).value())
                .containsExactly(ProjectManagerConst.FETCH_FEASIBILITY);
        assertThat(endpoint.getAnnotation(RoleConstraints.class).organisationRoles())
                .containsExactly(OrganisationRole.RESEARCHER, OrganisationRole.PROJECT_MANAGER_ADMIN);
        assertThat(endpoint.getAnnotation(StateConstraints.class).projectStates())
                .containsExactly(ProjectState.DRAFT, ProjectState.REVIEW);
        assertThat(endpoint.getAnnotation(ProjectConstraints.class).queryFormats())
                .containsExactly(QueryFormat.AST_DATA);
        assertThat(endpoint.getAnnotation(FrontendAction.class).action())
                .isEqualTo(ProjectManagerConst.FETCH_FEASIBILITY_ACTION);
    }

    @Test
    void returnsFocusResultDirectly() throws Exception {
        Project project = new Project();
        ProjectBridgehead bridgehead = new ProjectBridgehead();
        JsonNode result = objectMapper.readTree("{\"total\":42}");
        when(feasibilityService.fetchFeasibility(project, bridgehead)).thenReturn(Mono.just(result));

        ResponseEntity response = controller.fetchFeasibility(project, bridgehead);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(objectMapper.readTree((String) response.getBody())).isEqualTo(result);
        verify(feasibilityService).fetchFeasibility(project, bridgehead);
    }
}
