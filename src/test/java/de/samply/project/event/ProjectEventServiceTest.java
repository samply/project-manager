package de.samply.project.event;

import de.samply.db.model.Project;
import de.samply.db.model.Query;
import de.samply.notification.NotificationService;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectService;
import de.samply.project.code.ProjectCodeGenerator;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryService;
import de.samply.security.SessionUser;
import de.samply.user.UserService;
import de.samply.utils.LogUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectEventServiceTest {

    private ProjectService projectService;
    private ProjectCodeGenerator projectCodeGenerator;
    private ProjectBridgeheadService projectBridgeheadService;
    private ProjectEventService projectEventService;
    private final List<String> attemptedCodes = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        projectService = mock(ProjectService.class);
        projectCodeGenerator = mock(ProjectCodeGenerator.class);
        projectBridgeheadService = mock(ProjectBridgeheadService.class);
        QueryService queryService = mock(QueryService.class);
        when(queryService.fetchQuery("query")).thenReturn(Optional.of(new Query()));
        SessionUser sessionUser = mock(SessionUser.class);
        when(sessionUser.getEmail()).thenReturn("user@example.com");

        StateMachine<ProjectState, ProjectEvent> stateMachine = mock(StateMachine.class);
        State<ProjectState, ProjectEvent> state = mock(State.class);
        when(state.getId()).thenReturn(ProjectState.DRAFT);
        when(stateMachine.getState()).thenReturn(state);
        when(stateMachine.startReactively()).thenReturn(Mono.empty());
        StateMachineFactory<ProjectState, ProjectEvent> stateMachineFactory = mock(StateMachineFactory.class);
        when(stateMachineFactory.getStateMachine(anyString())).thenReturn(stateMachine);

        projectEventService = new ProjectEventService(mock(NotificationService.class), projectService,
                stateMachineFactory, mock(LogUtils.class), sessionUser, 90, projectCodeGenerator,
                mock(UserService.class), queryService, projectBridgeheadService);
    }

    private void failSaveFor(String code, String constraintName) {
        doAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            attemptedCodes.add(project.getCode());
            if (project.getCode().equals(code)) {
                throw new DataIntegrityViolationException("duplicate key",
                        new ConstraintViolationException("duplicate key", new SQLException(), constraintName));
            }
            return null;
        }).when(projectService).saveProject(any(Project.class));
    }

    @Test
    void retriesWithNewCodeWhenInsertHitsProjectCodeConstraint() throws Exception {
        when(projectCodeGenerator.generate()).thenReturn("REQ-1", "REQ-2");
        failSaveFor("REQ-1", "uq_project_code");

        String code = projectEventService.draft(new String[]{"bridgehead"}, "query");

        assertThat(code).isEqualTo("REQ-2");
        assertThat(attemptedCodes).containsExactly("REQ-1", "REQ-2");
        verify(projectBridgeheadService).saveBridgehead(any());
    }

    @Test
    void doesNotRetryOtherIntegrityViolations() throws Exception {
        when(projectCodeGenerator.generate()).thenReturn("REQ-1", "REQ-2");
        failSaveFor("REQ-1", "fk_project_query");

        projectEventService.draft(new String[]{"bridgehead"}, "query");

        assertThat(attemptedCodes).containsExactly("REQ-1");
        verify(projectBridgeheadService, never()).saveBridgehead(any());
    }
}
