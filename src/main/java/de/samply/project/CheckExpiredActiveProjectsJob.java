package de.samply.project;

import de.samply.app.ProjectManagerConst;
import de.samply.project.event.ProjectEventService;
import de.samply.project.state.ProjectState;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class CheckExpiredActiveProjectsJob {

    private final ProjectService projectService;
    private final ProjectEventService projectEventService;

    public CheckExpiredActiveProjectsJob(ProjectService projectService,
                                         ProjectEventService projectEventService) {
        this.projectService = projectService;
        this.projectEventService = projectEventService;
    }

    @Scheduled(cron = ProjectManagerConst.CHECK_EXPIRED_ACTIVE_PROJECTS_CRON_EXPRESSION_SV)
    public void checkExpiredActiveProjects() {
        projectService.findProjectByExpiresAtBeforeAndStateIn(LocalDate.now(),
                Set.of(ProjectState.REVIEW, ProjectState.APPROVAL, ProjectState.DEVELOP,
                        ProjectState.PILOT, ProjectState.FINAL)).forEach(projectEventService::archive);
    }

}
