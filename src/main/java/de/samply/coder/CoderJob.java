package de.samply.coder;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.exporter.ExporterService;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.query.QueryState;
import de.samply.register.AppRegisterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class CoderJob {

    // Services
    private final CoderService coderService;
    private final ExporterService exporterService;
    private final AppRegisterService appRegisterService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;

    public CoderJob(CoderService coderService,
                    ExporterService exporterService,
                    AppRegisterService appRegisterService,
                    ProjectBridgeheadUserService projectBridgeheadUserService
    ) {
        this.projectBridgeheadUserService = projectBridgeheadUserService;
        this.coderService = coderService;
        this.exporterService = exporterService;
        this.appRegisterService = appRegisterService;
    }

    @Scheduled(cron = ProjectManagerConst.CODER_CRON_EXPRESSION_SV)
    public void manageCoderWorkspaces() {
        log.debug("Starting Coder Job...");
        Mono.when(
                manageCoderActiveUsers(),
                manageCoderInactiveUsers()
        ).block();
        log.debug("Coder Job finished.");
    }

    public Mono<Void> manageCoderActiveUsers() {
        log.debug("Managing Coder Active Users...");
        return Flux.fromIterable(fetchActiveUsers())
                .flatMap(coderService::createWorkspace)
                .flatMap(projectCoder -> appRegisterService.register(projectCoder).then(Mono.just(projectCoder)))
                .flatMap(exporterService::transferFileToResearchEnvironment)
                .then();
    }

    private List<ProjectBridgeheadUser> fetchActiveUsers() {
        return projectBridgeheadUserService.fetchUsersInValidProjectState(ProjectType.RESEARCH_ENVIRONMENT, QueryState.FINISHED, ProjectBridgeheadState.ACCEPTED);
    }

    public Mono<Void> manageCoderInactiveUsers() {
        log.debug("Managing Coder Inactive Users...");
        return Flux.fromIterable(fetchInactiveUsers())
                .flatMap(coderService::deleteWorkspace)
                .flatMap(appRegisterService::unregister)
                .then();
    }

    private List<ProjectBridgeheadUser> fetchInactiveUsers() {
        return projectBridgeheadUserService.fetchUsersInInvalidProjectState(ProjectType.RESEARCH_ENVIRONMENT);
    }

}
