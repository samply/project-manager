package de.samply.project;

import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Project;
import de.samply.frontend.dto.ProjectAndForms;
import de.samply.frontend.dto.Results;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.project.state.ProjectState;
import de.samply.security.SessionUser;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DtoProjectService {

    private final ProjectService projectService;
    private final DtoFactory dtoFactory;
    private final SessionUser sessionUser;
    private final ProjectConfigurations projectConfigurations;

    public DtoProjectService(
            ProjectService projectService,
            DtoFactory dtoFactory,
            SessionUser sessionUser,
            ProjectConfigurations projectConfigurations) {
        this.projectService = projectService;
        this.dtoFactory = dtoFactory;
        this.sessionUser = sessionUser;
        this.projectConfigurations = projectConfigurations;
    }

    public Project fetchDtoProject(@NotNull String projectCode) throws ProjectServiceException {
        return dtoFactory.convert(projectService.fetchProject(projectCode));
    }

    public Page<Project> fetchUserVisibleProjects(
            Optional<ProjectState> projectState, Optional<Boolean> archived, int page, int pageSize,
            boolean modifiedDescendant) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        if (projectService.isProjectManagerAdmin()) {
            return projectService.fetchProjectManagerAdminProjects(projectState, archived, pageRequest, modifiedDescendant).map(dtoFactory::convert);
        }
        Set<String> bridgeheads = sessionUser.getBridgeheads();
        // We make an assumption: A bridgehead admin is bridgehead admin in all of their bridgeheads.
        if (projectService.isBridgeheadAdmin()) {
            return projectService.fetchBridgeheadAdminProjects(bridgeheads, projectState, archived, pageRequest, modifiedDescendant).map(dtoFactory::convert);
        }
        return projectService.fetchResearcherProjects(sessionUser.getEmail(), bridgeheads, projectState, archived, pageRequest, modifiedDescendant).map(dtoFactory::convert);
    }

    public Map<String, ProjectAndForms> fetchCurrentProjectConfiguration(@NotNull de.samply.db.model.Project project) throws ProjectServiceException {
        return this.projectConfigurations.fetchCurrentProjectConfiguration(dtoFactory.convertToProjectAndForms(project, Optional.empty()));
    }

    public Optional<Results> fetchResults(@NotNull de.samply.db.model.Project project) throws ProjectServiceException {
        return dtoFactory.fetchResults(project);
    }


}
