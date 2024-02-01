package de.samply.project;

import de.samply.app.ProjectManagerConst;
import de.samply.db.repository.ProjectRepository;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectState;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class CheckExpiredActiveProjectsJob {

    private final ProjectRepository projectRepository;
    private final ProjectEventService projectEventService;

    public CheckExpiredActiveProjectsJob(ProjectRepository projectRepository,
                                         ProjectEventService projectEventService) {
        this.projectRepository = projectRepository;
        this.projectEventService = projectEventService;
    }

    @Scheduled(cron = ProjectManagerConst.CHECK_EXPIRED_ACTIVE_PROJECTS_CRON_EXPRESSION_SV)
    public void checkExpiredActiveProjects() {
        projectRepository.findByExpiresAtBeforeAndStateIn(LocalDate.now(),
                Set.of(ProjectState.CREATED, ProjectState.ACCEPTED, ProjectState.DEVELOP,
                        ProjectState.PILOT, ProjectState.FINAL)).forEach(expiredActiveProject ->
                projectEventService.archive(expiredActiveProject.getCode()));
    }

}
