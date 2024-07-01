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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CoderJob {

    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final ProjectCoderRepository projectCoderRepository;
    private final CoderService coderService;
    private final ExporterService exporterService;

    public CoderJob(ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                    ProjectCoderRepository projectCoderRepository,
                    CoderService coderService,
                    ExporterService exporterService
    ) {
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.projectCoderRepository = projectCoderRepository;
        this.coderService = coderService;
        this.exporterService = exporterService;
    }


    @Scheduled(cron = ProjectManagerConst.CODER_CRON_EXPRESSION_SV)
    public void manageCoderWorkspaces() {
        manageCoderActiveUsers();
        manageCoderInactiveUsers();
    }

    public void manageCoderActiveUsers() {
        fetchActiveUsers().stream().forEach(user -> {
            Optional<ProjectCoder> projectCoder = projectCoderRepository.findByProjectBridgeheadUserAndDeletedAtIsNull(user);
            if (projectCoder.isEmpty()) {
                this.coderService.createWorkspace(user);
            } else if (!projectCoder.get().isExportTransferred()) {
                exporterService.transferFileToCoder(user.getProjectBridgehead(), projectCoder.get()).subscribe(result -> {
                    projectCoder.get().setExportTransferred(true);
                    this.projectCoderRepository.save(projectCoder.get());
                });
            }
        });
    }

    private List<ProjectBridgeheadUser> fetchActiveUsers() {
        return projectBridgeheadUserRepository.getDistinctInValidaProjectStateByProjectTypeAndQueryStateAndProjectBridgeheadState(ProjectType.RESEARCH_ENVIRONMENT, QueryState.FINISHED, ProjectBridgeheadState.ACCEPTED);
    }

    public void manageCoderInactiveUsers() {
        fetchInactiveUsers().stream().forEach(user -> {
            Optional<ProjectCoder> projectCoder = projectCoderRepository.findByProjectBridgeheadUserAndDeletedAtIsNull(user);
            if (projectCoder.isPresent()) {
                this.coderService.deleteWorkspace(user);
                projectCoder.get().setDeletedAt(Instant.now());
                projectCoderRepository.save(projectCoder.get());
            }
        });
    }

    private List<ProjectBridgeheadUser> fetchInactiveUsers() {
        return projectBridgeheadUserRepository.getDistinctInInvalidProjectStateByProjectType(ProjectType.RESEARCH_ENVIRONMENT);
    }

}
