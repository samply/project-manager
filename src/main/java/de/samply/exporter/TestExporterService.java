package de.samply.exporter;

import de.samply.annotations.ConditionalOnModuleTest;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadExecution;
import de.samply.db.model.ProjectCoder;
import de.samply.modules.ModuleMode;
import de.samply.modules.OptionalModule;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectType;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Test mode of EXPORTER (ENABLE_EXPORTER=test), for development without Beam and bridgeheads: every step answers at
 * once, so the real {@link ExporterJob} moves the queries through all their states (sent, executed, finished) with
 * the usual notifications and emails. Nothing is exported; the exporter response and the execution ID are fake.
 */
@Slf4j
@Service
@ConditionalOnModuleTest(OptionalModule.EXPORTER)
public class TestExporterService implements ExporterService {

    static final String TEST_TEMPLATE = "test";
    static final String TEST_RESPONSE = "Test exporter: nothing was exported";

    private final NotificationService notificationService;

    public TestExporterService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public Map<ProjectType, Set<String>> getExporterTemplates() {
        return Map.of(ProjectType.EXPORT, Set.of(TEST_TEMPLATE), ProjectType.SAMPLES, Set.of(TEST_TEMPLATE),
                ProjectType.DATASHIELD, Set.of(TEST_TEMPLATE), ProjectType.RESEARCH_ENVIRONMENT, Set.of(TEST_TEMPLATE));
    }

    @Override
    public Mono<ExporterServiceResult> sendQueryToBridgehead(ProjectBridgeheadAndType projectBridgeheadAndType) {
        return answer(projectBridgeheadAndType, "query sent", Optional.of(OperationType.SEND_QUERY_TO_BRIDGEHEAD));
    }

    @Override
    public Mono<ExporterServiceResult> sendQueryToBridgeheadAndExecute(ProjectBridgeheadAndType projectBridgeheadAndType) {
        return answer(projectBridgeheadAndType, "query sent to be executed",
                Optional.of(OperationType.SEND_QUERY_TO_BRIDGEHEAD_AND_EXECUTE));
    }

    @Override
    public Mono<ExporterServiceResult> checkExecutionStatus(ProjectBridgeheadAndType projectBridgeheadAndType) {
        return answer(projectBridgeheadAndType, "export finished", Optional.empty());
    }

    @Override
    public Mono<ExporterServiceResult> checkIfQueryIsAlreadySentOrExecuted(ProjectBridgeheadAndType projectBridgeheadAndType) {
        // The same notification as the Beam implementation, depending on the step
        Optional<OperationType> operationType = projectBridgeheadAndType.projectBridgehead()
                .fetchExecution(projectBridgeheadAndType.projectType())
                .flatMap(execution -> switch (execution.getQueryState()) {
                    case SENDING -> Optional.of(OperationType.CHECK_SEND_QUERY);
                    case SENDING_AND_EXECUTING -> Optional.of(OperationType.CHECK_SEND_AND_EXECUTE_QUERY);
                    default -> Optional.empty();
                });
        return answer(projectBridgeheadAndType, "query received by the bridgehead", operationType);
    }

    @Override
    public Optional<String> fetchExporterExecutionIdFromExporterResponse(String exporterResponse) {
        return Optional.of("test-execution");
    }

    @Override
    public void transferFileToResearchEnvironment(@NotNull String projectCode, @NotNull String bridgehead) {
        throw createNoExportException("transfer the export file of project " + projectCode + " and bridgehead " + bridgehead);
    }

    @Override
    public Mono<Void> transferFileToResearchEnvironment(ProjectCoder projectCoder) {
        return Mono.error(createNoExportException("transfer an export file"));
    }

    @Override
    public boolean isExportFileTransferredToResearchEnvironment(@NotNull String projectCode, @NotNull String bridgehead) {
        return false;
    }

    private Mono<ExporterServiceResult> answer(ProjectBridgeheadAndType projectBridgeheadAndType, String step,
                                               Optional<OperationType> operationType) {
        ProjectBridgehead bridgehead = projectBridgeheadAndType.projectBridgehead();
        log.info("Test exporter: {} - project {}, bridgehead {}, type {}", step, bridgehead.getProject().getCode(),
                bridgehead.getBridgehead(), projectBridgeheadAndType.projectType());
        operationType.ifPresent(type -> notificationService.createNotification(bridgehead.getProject(),
                bridgehead.getBridgehead(), fetchExporterUser(projectBridgeheadAndType), type,
                "Test exporter: " + step, null, HttpStatus.OK));
        return Mono.just(new ExporterServiceResult(projectBridgeheadAndType, TEST_RESPONSE));
    }

    private String fetchExporterUser(ProjectBridgeheadAndType projectBridgeheadAndType) {
        return projectBridgeheadAndType.projectBridgehead().fetchExecution(projectBridgeheadAndType.projectType())
                .map(ProjectBridgeheadExecution::getExporterUser)
                .orElse(null);
    }

    // No export exists in test mode, so there is no file to transfer into a research environment
    private IllegalStateException createNoExportException(String action) {
        return new IllegalStateException("Exporter in " + ModuleMode.TEST + " mode: cannot " + action);
    }

}
