package de.samply.exporter;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadExecution;
import de.samply.email.EmailKeyValuesFactory;
import de.samply.email.EmailService;
import de.samply.email.EmailTemplateType;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryState;
import de.samply.user.UserService;
import de.samply.user.roles.ProjectRole;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Component
public class ExporterJob {

    private final boolean enabled;
    private final Set<ProjectState> activeStates = Set.of(ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL);

    // Services
    private final ExporterService exporterService;
    private final EmailService emailService;
    private final UserService userService;
    private final ProjectBridgeheadService projectBridgeheadService;

    private final EmailKeyValuesFactory emailKeyValuesFactory;


    public ExporterJob(
            @Value(ProjectManagerConst.ENABLE_EXPORTER_SV) boolean enabled,
            ExporterService exporterService,
            EmailService emailService,
            UserService userService,
            ProjectBridgeheadService projectBridgeheadService,
            EmailKeyValuesFactory emailKeyValuesFactory) {
        this.enabled = enabled;
        this.exporterService = exporterService;
        this.projectBridgeheadService = projectBridgeheadService;
        this.emailService = emailService;
        this.userService = userService;
        this.emailKeyValuesFactory = emailKeyValuesFactory;
    }

    @Scheduled(cron = ProjectManagerConst.EXPORTER_CRON_EXPRESSION_SV)
    @SchedulerLock(name = ProjectManagerConst.EXPORTER_JOB_NAME)
    public void checkExports() {
        if (enabled) {
            log.debug("Exporter Job started");
            Mono.when(
                    checkQueriesToSend(),
                    checkQueriesToSendAndExecute(),
                    checkQueriesAlreadySent(),
                    checkQueriesAlreadySentToBeExecuted(),
                    checkQueriesAlreadyExecutingStep1(),
                    checkQueriesAlreadyExecutingStep2()).block();
            log.debug("Exporter Job finished");
        }
    }

    private Mono<Void> checkQueriesToSend() {
        log.debug("Checking queries to send...");
        return checkQueries(QueryState.TO_BE_SENT, QueryState.SENDING, exporterService::sendQueryToBridgehead);
    }

    private Mono<Void> checkQueriesToSendAndExecute() {
        log.debug("Checking queries to send and execute...");
        return checkQueries(QueryState.TO_BE_SENT_AND_EXECUTED, QueryState.SENDING_AND_EXECUTING, exporterService::sendQueryToBridgeheadAndExecute);
    }

    private Mono<Void> checkQueriesAlreadySent() {
        log.debug("Checking queries already sent...");
        return checkQueries(QueryState.SENDING, exporterService::checkIfQueryIsAlreadySentOrExecuted, EmailTemplateType.QUERY_SAVED_IN_EXPORTER);
    }

    private Mono<Void> checkQueriesAlreadyExecutingStep1() {
        log.debug("Checking queries already executing (step 1)...");
        return checkQueries(QueryState.EXPORT_RUNNING_1, QueryState.EXPORT_RUNNING_2, exporterService::checkExecutionStatus);
    }

    private Mono<Void> checkQueriesAlreadyExecutingStep2() {
        log.debug("Checking queries already executing (step 2)...");
        return checkQueries(QueryState.EXPORT_RUNNING_2, exporterService::checkIfQueryIsAlreadySentOrExecuted, EmailTemplateType.QUERY_SAVED_IN_EXPORTER_AND_EXECUTED);
    }

    private Mono<Void> checkQueriesAlreadySentToBeExecuted() {
        log.debug("Checking queries already sent to be executed...");
        return checkQueries(QueryState.SENDING_AND_EXECUTING, QueryState.EXPORT_RUNNING_1, exporterService::checkIfQueryIsAlreadySentOrExecuted, Optional.of(
                exporterServiceResult ->
                        exporterService.fetchExporterExecutionIdFromExporterResponse(exporterServiceResult.result()).ifPresent(exportExecutionId ->
                                exporterServiceResult
                                        .projectBridgeheadAndType()
                                        .projectBridgehead()
                                        .fetchExecution(exporterServiceResult.projectBridgeheadAndType().projectType())
                                        .orElseThrow(() -> new IllegalStateException("Missing execution for " + exporterServiceResult.projectBridgeheadAndType().projectType()))
                                        .setExporterExecutionId(exportExecutionId))), Optional.empty());
    }

    private Mono<Void> checkQueries(QueryState initialQueryState, QueryState finalQueryState,
                                    Function<ProjectBridgeheadAndType, Mono<ExporterServiceResult>> exporterServiceFunction) {
        return checkQueries(initialQueryState, finalQueryState, exporterServiceFunction, Optional.empty(), Optional.empty());
    }

    private Mono<Void> checkQueries(QueryState initialQueryState,
                                    Function<ProjectBridgeheadAndType, Mono<ExporterServiceResult>> exporterServiceFunction, EmailTemplateType emailTemplateType) {
        return checkQueries(initialQueryState, QueryState.FINISHED, exporterServiceFunction, Optional.empty(), Optional.of(emailTemplateType));
    }


    private Mono<Void> checkQueries(QueryState initialQueryState,
                                    QueryState finalQueryState,
                                    Function<ProjectBridgeheadAndType, Mono<ExporterServiceResult>> exporterServiceFunction,
                                    Optional<Consumer<ExporterServiceResult>> exporterServiceResultConsumer,
                                    Optional<EmailTemplateType> emailTemplateType) {
        return Flux.fromStream(
                        projectBridgeheadService
                                .fetchBridgeheads(initialQueryState, activeStates)
                                .stream()
                                .flatMap(ProjectBridgeheadAndType::from)
                )
                .flatMap(exporterServiceFunction)
                .doOnNext(exporterServiceResult -> {
                    exporterServiceResultConsumer.ifPresent(consumer -> consumer.accept(exporterServiceResult));
                    ProjectBridgehead projectBridgehead = exporterServiceResult.projectBridgeheadAndType().projectBridgehead();
                    ProjectBridgeheadExecution execution = projectBridgehead
                            .fetchExecution(exporterServiceResult.projectBridgeheadAndType().projectType())
                            .orElseThrow(() -> new IllegalStateException("Missing execution for " + exporterServiceResult.projectBridgeheadAndType().projectType()));
                    log.debug("Setting final query state and updating exporter response for project {} and bridgehead {}", projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead());
                    execution.setQueryState(finalQueryState);
                    execution.setExporterResponse(exporterServiceResult.result());
                    if (finalQueryState == QueryState.FINISHED) {
                        execution.setExporterDispatchCounter(execution.getExporterDispatchCounter() + 1);
                    }
                    projectBridgeheadService.saveBridgehead(projectBridgehead);
                    emailTemplateType.ifPresent(type -> sendEmail(projectBridgehead, type));
                })
                .then();
    }

    private void sendEmail(ProjectBridgehead projectBridgehead, EmailTemplateType templateType) {
        userService.fetchBridgeheadAdmin(projectBridgehead)
                .forEach(bridgeheadAdmin ->
                        emailService.sendEmail(
                                bridgeheadAdmin.getEmail(),
                                Optional.of(projectBridgehead.getProject()),
                                Optional.of(projectBridgehead),
                                ProjectRole.BRIDGEHEAD_ADMIN,
                                templateType,
                                emailKeyValuesFactory.newInstance().add(projectBridgehead)));
    }

}
