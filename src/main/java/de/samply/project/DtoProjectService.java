package de.samply.project;

import de.samply.db.model.Project_;
import de.samply.db.model.Query_;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Project;
import de.samply.frontend.dto.ProjectAndForms;
import de.samply.frontend.dto.Results;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.project.state.ProjectState;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DtoProjectService {

    private final ProjectService projectService;
    private final DtoFactory dtoFactory;
    private final ProjectConfigurations projectConfigurations;

    public DtoProjectService(
            ProjectService projectService,
            DtoFactory dtoFactory,
            ProjectConfigurations projectConfigurations) {
        this.projectService = projectService;
        this.dtoFactory = dtoFactory;
        this.projectConfigurations = projectConfigurations;
    }

    public Project fetchDtoProject(@NotNull de.samply.db.model.Project project) throws ProjectServiceException {
        return dtoFactory.convert(project);
    }

    public Page<Project> fetchUserVisibleProjects(
            Optional<ProjectState> projectState, Optional<Boolean> archived, int page, int pageSize,
            ProjectSortField sortBy, boolean sortDesc, Optional<String> projectCreator, Optional<String> bridgehead) {
        Sort.Direction direction = sortDesc ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = switch (sortBy) {
            case TITLE -> Project_.QUERY + "." + Query_.LABEL;
            case REQUEST_ID -> Project_.CODE;
            case PROJECT_CREATOR -> Project_.CREATOR_EMAIL;
            case STATUS -> Project_.STATE;
            case CREATED -> Project_.CREATED_AT;
            case MODIFIED_AT -> Project_.MODIFIED_AT;
        };
        PageRequest pageRequest = PageRequest.of(
                page, pageSize, Sort.by(direction, sortProperty));
        return projectService.fetchUserVisibleProjects(
                        projectState, archived, pageRequest, projectCreator, bridgehead)
                .map(dtoFactory::convert);
    }

    public List<String> fetchCurrentProjectConfiguration(@NotNull de.samply.db.model.Project project) throws ProjectServiceException {
        return this.projectConfigurations.fetchCurrentProjectConfiguration(dtoFactory.convertToProjectAndForms(project, Optional.empty()));
    }

    public List<ProjectAndForms> fetchCurrentProjectConfigurations(@NotNull de.samply.db.model.Project project) throws ProjectServiceException {
        return this.projectConfigurations.fetchCurrentProjectConfigurations(
                dtoFactory.convertToProjectAndForms(project, Optional.empty()));
    }

    public Optional<Results> fetchResults(@NotNull de.samply.db.model.Project project) throws ProjectServiceException {
        return dtoFactory.fetchResults(project);
    }


}
