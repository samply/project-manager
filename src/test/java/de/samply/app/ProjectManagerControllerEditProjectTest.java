package de.samply.app;

import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.Project;
import de.samply.frontend.FrontendService;
import de.samply.project.ProjectService;
import de.samply.query.QueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectManagerControllerEditProjectTest {

    @Mock
    private FrontendService frontendService;
    @Mock
    private QueryService queryService;
    @Mock
    private ProjectService projectService;
    @Mock
    private BridgeheadConfiguration bridgeheadConfiguration;
    @InjectMocks
    private ProjectManagerController controller;

    @Test
    void updatesBridgeheadsWhenOnlyExplorerIdsAreProvided() {
        Project project = new Project();
        project.setCode("project-code");
        String[] explorerIds = {"lens-essen", "lens-frankfurt"};
        when(bridgeheadConfiguration.getBridgeheadForExplorerId("lens-essen"))
                .thenReturn(Optional.of("essen"));
        when(bridgeheadConfiguration.getBridgeheadForExplorerId("lens-frankfurt"))
                .thenReturn(Optional.of("frankfurt"));
        when(frontendService.fetchExplorerRedirectUri(
                ProjectManagerConst.PROJECT_VIEW_SITE,
                Map.of(ProjectManagerConst.PROJECT_CODE, project.getCode())))
                .thenReturn(Map.of("url", "project-url"));

        var response = controller.editProject(
                null, null, null, explorerIds, null, null, null, null, null,
                null, null, null, null, null, project);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(projectService).updateBridgeheads(
                project, new String[]{"essen", "frankfurt"});
    }
}
