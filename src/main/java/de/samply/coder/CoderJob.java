package de.samply.coder;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.ProjectCoder;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectCoderRepository;
import de.samply.exporter.ExporterService;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.query.QueryState;
import de.samply.register.AppRegisterService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

    // TODO: Assure that asynchronous processes finish !!!!!!!!!!!!!!!!!!!!!!!!!!

    @Scheduled(cron = ProjectManagerConst.CODER_CRON_EXPRESSION_SV)
    public void manageCoderWorkspaces() {
        manageCoderActiveUsers();
        manageCoderInactiveUsers();
    }

    public void manageCoderActiveUsers() {
        fetchActiveUsers().stream().forEach(user -> {
            Optional<ProjectCoder> projectCoder = projectCoderRepository.findFirstByProjectBridgeheadUserAndDeletedAtIsNullOrderByCreatedAtDesc(user);
            if (projectCoder.isEmpty()) {
                this.coderService.createWorkspace(user);
                this.appRegisterService.register(projectCoder.get());
            } else if (!projectCoder.get().isExportTransferred()) {
                exporterService.transferFileToResearchEnvironment(projectCoder.get());
            }
        });
    }

    private List<ProjectBridgeheadUser> fetchActiveUsers() {
        return projectBridgeheadUserRepository.getDistinctInValidaProjectStateByProjectTypeAndQueryStateAndProjectBridgeheadState(ProjectType.RESEARCH_ENVIRONMENT, QueryState.FINISHED, ProjectBridgeheadState.ACCEPTED);
    }

    public void manageCoderInactiveUsers() {
        fetchInactiveUsers().stream().forEach(user -> {
            Optional<ProjectCoder> projectCoder = projectCoderRepository.findFirstByProjectBridgeheadUserAndDeletedAtIsNullOrderByCreatedAtDesc(user);
            if (projectCoder.isPresent()) {
                this.coderService.deleteWorkspace(user);
                this.appRegisterService.unregister(projectCoder.get());
                projectCoder.get().setDeletedAt(Instant.now());
                projectCoderRepository.save(projectCoder.get());
            }
        });
    }

    private List<ProjectBridgeheadUser> fetchInactiveUsers() {
        return projectBridgeheadUserRepository.getDistinctInInvalidProjectStateByProjectType(ProjectType.RESEARCH_ENVIRONMENT);
    }

}
