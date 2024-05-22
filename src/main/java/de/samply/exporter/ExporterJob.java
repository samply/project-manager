package de.samply.exporter;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class ExporterJob {

    private final boolean enabled;
    private final ExporterService exporterService;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final Set<ProjectState> activeStates = Set.of(ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL);

    public ExporterJob(
            @Value(ProjectManagerConst.ENABLE_EXPORTER_SV) boolean enabled,
            ExporterService exporterService,
            ProjectBridgeheadRepository projectBridgeheadRepository) {
        this.enabled = enabled;
        this.exporterService = exporterService;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
    }

    @Scheduled(cron = ProjectManagerConst.EXPORTER_CRON_EXPRESSION_SV)
    public void checkExports() {
        if (enabled) {
            Mono.when(
                    checkQueriesToSend(),
                    checkQueriesToSendAndExecute(),
                    checkQueriesAlreadySent(),
                    checkQueriesAlreadySentToBeExecuted(),
                    checkQueriesAlreadyExecutingStep1(),
                    checkQueriesAlreadyExecutingStep2()).block();

        }
    }

    private Mono<Void> checkQueriesToSend() {
        return checkQueries(QueryState.TO_BE_SENT, QueryState.SENDING, exporterService::sendQueryToBridgehead);
    }

    private Mono<Void> checkQueriesToSendAndExecute() {
        return checkQueries(QueryState.TO_BE_SENT_AND_EXECUTED, QueryState.SENDING_AND_EXECUTING, exporterService::sendQueryToBridgeheadAndExecute);
    }

    private Mono<Void> checkQueriesAlreadySent() {
        return checkQueries(QueryState.SENDING, QueryState.FINISHED, exporterService::checkIfQueryIsAlreadySentOrExecuted);
    }

    private Mono<Void> checkQueriesAlreadyExecutingStep1() {
        return checkQueries(QueryState.EXPORT_RUNNING_1, QueryState.EXPORT_RUNNING_2, exporterService::checkExecutionStatus);
    }

    private Mono<Void> checkQueriesAlreadyExecutingStep2() {
        return checkQueries(QueryState.EXPORT_RUNNING_2, QueryState.FINISHED, exporterService::checkIfQueryIsAlreadySentOrExecuted);
    }

    private Mono<Void> checkQueriesAlreadySentToBeExecuted() {
        return checkQueries(QueryState.SENDING_AND_EXECUTING, QueryState.EXPORT_RUNNING_1, exporterService::checkIfQueryIsAlreadySentOrExecuted, Optional.of(
                exporterServiceResult ->
                        exporterService.fetchExporterExecutionIdFromExporterResponse(exporterServiceResult.result()).ifPresent(exportExecutionId ->
                                exporterServiceResult.projectBridgehead().setExporterExecutionId(exportExecutionId))));
    }

    private Mono<Void> checkQueries(QueryState initialQueryState, QueryState finalQueryState,
                                    Function<ProjectBridgehead, Mono<ExporterServiceResult>> exporterServiceFunction) {
        return checkQueries(initialQueryState, finalQueryState, exporterServiceFunction, Optional.empty());
    }

    private Mono<Void> checkQueries(QueryState initialQueryState, QueryState finalQueryState,
                                    Function<ProjectBridgehead, Mono<ExporterServiceResult>> exporterServiceFunction, Optional<Consumer<ExporterServiceResult>> exporterServiceResultConsumer) {
        return Flux.fromIterable(projectBridgeheadRepository.getByQueryStateAndProjectState(initialQueryState, activeStates))
                .flatMap(exporterServiceFunction)
                .flatMap(exporterServiceResult -> {
                    exporterServiceResultConsumer.ifPresent(consumer -> consumer.accept(exporterServiceResult));
                    ProjectBridgehead projectBridgehead = exporterServiceResult.projectBridgehead();
                    projectBridgehead.setQueryState(finalQueryState);
                    projectBridgehead.setExporterResponse(exporterServiceResult.result());
                    projectBridgehead.setModifiedAt(Instant.now());
                    projectBridgeheadRepository.save(projectBridgehead);
                    return Mono.empty();
                }).then();
    }

}
