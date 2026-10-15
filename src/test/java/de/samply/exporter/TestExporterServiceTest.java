package de.samply.exporter;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadExecution;
import de.samply.db.model.QueryOutput;
import de.samply.email.EmailKeyValuesFactory;
import de.samply.email.EmailSendingService;
import de.samply.email.EmailTemplateType;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectType;
import de.samply.query.QueryState;
import de.samply.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The real {@link ExporterJob} with {@link TestExporterService}: the queries go through all their states without Beam.
 */
class TestExporterServiceTest {

    private NotificationService notificationService;
    private EmailSendingService emailSendingService;
    private ProjectBridgeheadService projectBridgeheadService;
    private ExporterJob exporterJob;
    private ProjectBridgehead bridgehead;
    private ProjectBridgeheadExecution execution;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        emailSendingService = mock(EmailSendingService.class);
        projectBridgeheadService = mock(ProjectBridgeheadService.class);
        // The job asks for the bridgeheads whose query is in a state; this one answers with its current state
        when(projectBridgeheadService.fetchBridgeheads(any(QueryState.class), any()))
                .thenAnswer(invocation -> execution.getQueryState() == invocation.getArgument(0) ? Set.of(bridgehead) : Set.of());
        UserService userService = mock(UserService.class);
        when(userService.fetchBridgeheadAdmin(any())).thenReturn(Set.of());
        exporterJob = new ExporterJob(new TestExporterService(notificationService), emailSendingService, userService,
                projectBridgeheadService, mock(EmailKeyValuesFactory.class, RETURNS_DEEP_STUBS));

        Project project = new Project();
        project.setCode("REQ-2026-0001");
        bridgehead = new ProjectBridgehead();
        bridgehead.setProject(project);
        bridgehead.setBridgehead("site-a");
        QueryOutput output = new QueryOutput();
        output.setProjectType(ProjectType.EXPORT);
        execution = new ProjectBridgeheadExecution();
        execution.setQueryOutput(output);
        execution.setExporterUser("researcher@example.org");
        bridgehead.getExecutions().add(execution);
    }

    @Test
    void sendsAQueryUntilItIsFinished() {
        execution.setQueryState(QueryState.TO_BE_SENT);

        exporterJob.checkExports();
        assertThat(execution.getQueryState()).isEqualTo(QueryState.SENDING);
        exporterJob.checkExports();
        assertThat(execution.getQueryState()).isEqualTo(QueryState.FINISHED);

        assertThat(execution.getExporterResponse()).isEqualTo(TestExporterService.TEST_RESPONSE);
        verify(notificationService).createNotification(any(), eq("site-a"), eq("researcher@example.org"),
                eq(OperationType.SEND_QUERY_TO_BRIDGEHEAD), anyString(), any(), any());
        verify(notificationService).createNotification(any(), eq("site-a"), eq("researcher@example.org"),
                eq(OperationType.CHECK_SEND_QUERY), anyString(), any(), any());
    }

    @Test
    void sendsAndExecutesAQueryUntilItIsFinished() {
        execution.setQueryState(QueryState.TO_BE_SENT_AND_EXECUTED);

        exporterJob.checkExports();
        assertThat(execution.getQueryState()).isEqualTo(QueryState.SENDING_AND_EXECUTING);
        exporterJob.checkExports();
        assertThat(execution.getQueryState()).isEqualTo(QueryState.EXPORT_RUNNING_1);
        assertThat(execution.getExporterExecutionId()).isEqualTo("test-execution");
        exporterJob.checkExports();
        assertThat(execution.getQueryState()).isEqualTo(QueryState.EXPORT_RUNNING_2);
        exporterJob.checkExports();
        assertThat(execution.getQueryState()).isEqualTo(QueryState.FINISHED);
    }

    @Test
    void offersTheTestTemplateAndTransfersNothing() {
        TestExporterService testExporterService = new TestExporterService(notificationService);

        assertThat(testExporterService.getExporterTemplates().get(ProjectType.EXPORT)).containsExactly(TestExporterService.TEST_TEMPLATE);
        assertThat(testExporterService.isExportFileTransferredToResearchEnvironment("REQ-2026-0001", "site-a")).isFalse();
        verify(emailSendingService, never()).sendEmail(anyString(), any(), any(), any(), any(EmailTemplateType.class), any());
    }

}
