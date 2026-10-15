package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Results;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DtoProjectBridgeheadService {

    // Services
    private final ProjectService projectService;
    private final ProjectBridgeheadService projectBridgeheadService;

    private final DtoFactory dtoFactory;


    public DtoProjectBridgeheadService(ProjectBridgeheadService projectBridgeheadService, DtoFactory dtoFactory,
                                      ProjectService projectService) {
        this.projectBridgeheadService = projectBridgeheadService;
        this.dtoFactory = dtoFactory;
        this.projectService = projectService;
    }

    public List<de.samply.frontend.dto.ProjectBridgehead> fetchUserVisibleProjectBridgeheads(Optional<Project> projectOptional)
            throws ProjectBridgeheadServiceException {
        Map<String, ProjectBridgehead> bridgeheads = projectOptional.map(List::of)
                .orElseGet(projectService::fetchAllUserVisibleProjects).stream()
                .flatMap(currentProject -> projectBridgeheadService.fetchBridgeheads(currentProject).stream()
                        .filter(bridgehead -> projectBridgeheadService.isProjectManagerAdmin()
                                || projectBridgeheadService.isBridgeheadAdminOfProjectBridgehead(bridgehead)
                                || projectBridgeheadService.isUserOfProjectBridgehead(bridgehead)
                                || projectBridgeheadService.isUserCreatorOfProject(currentProject)))
                .collect(Collectors.toMap(ProjectBridgehead::getBridgehead, Function.identity(), (first, ignored) -> first));
        return bridgeheads.values().stream().map(dtoFactory::convert).toList();
    }

    public List<Results> fetchResults(@NotNull Project project) throws ProjectBridgeheadServiceException {
        return projectBridgeheadService
                .fetchBridgeheads(project)
                .stream()
                .map(dtoFactory::fetchResults)
                .toList(); // Uses the modern `toList()` instead of `Collectors.toList()`
    }

    public List<de.samply.frontend.dto.ProjectBridgehead> fetchProjectBridgeheads(@NotNull Project project) throws ProjectBridgeheadServiceException {
        return projectBridgeheadService
                .fetchBridgeheads(project)
                .stream()
                .map(dtoFactory::convert)
                .toList();
    }

    public Results fetchResultsOfOwnBridgehead(@NotNull ProjectBridgehead bridgehead) throws ProjectBridgeheadServiceException {
        return dtoFactory.fetchResults(bridgehead);
    }

}
