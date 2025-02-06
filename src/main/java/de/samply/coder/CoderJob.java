package de.samply.coder;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectCoderRepository;
import de.samply.exporter.ExporterService;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.query.QueryState;
import de.samply.register.AppRegisterService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class CoderJob {

    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final ProjectCoderRepository projectCoderRepository;
    private final CoderService coderService;
    private final ExporterService exporterService;
    private final AppRegisterService appRegisterService;

    public CoderJob(ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                    ProjectCoderRepository projectCoderRepository,
                    CoderService coderService,
                    ExporterService exporterService,
                    AppRegisterService appRegisterService
    ) {
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.projectCoderRepository = projectCoderRepository;
        this.coderService = coderService;
        this.exporterService = exporterService;
        this.appRegisterService = appRegisterService;
    }

    @Scheduled(cron = ProjectManagerConst.CODER_CRON_EXPRESSION_SV)
    public void manageCoderWorkspaces() {
        Mono.when(
                manageCoderActiveUsers(),
                manageCoderInactiveUsers()
        ).block();
    }

    public Mono<Void> manageCoderActiveUsers() {
        return Flux.fromIterable(fetchActiveUsers())
                .flatMap(coderService::createWorkspace)
                .flatMap(projectCoder -> appRegisterService.register(projectCoder).then(Mono.just(projectCoder)))
                .flatMap(exporterService::transferFileToResearchEnvironment)
                .then();
    }

    private List<ProjectBridgeheadUser> fetchActiveUsers() {
        return projectBridgeheadUserRepository.getDistinctInValidaProjectStateByProjectTypeAndQueryStateAndProjectBridgeheadState(ProjectType.RESEARCH_ENVIRONMENT, QueryState.FINISHED, ProjectBridgeheadState.ACCEPTED);
    }

    public Mono<Void> manageCoderInactiveUsers() {
        return Flux.fromIterable(fetchInactiveUsers())
                .flatMap(coderService::deleteWorkspace)
                .flatMap(appRegisterService::unregister)
                .then();
    }

    private List<ProjectBridgeheadUser> fetchInactiveUsers() {
        return projectBridgeheadUserRepository.getDistinctInInvalidProjectStateByProjectType(ProjectType.RESEARCH_ENVIRONMENT);
    }

}
