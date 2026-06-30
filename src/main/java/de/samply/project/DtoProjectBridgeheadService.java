package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Results;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DtoProjectBridgeheadService {

    private final ProjectBridgeheadService projectBridgeheadService;
    private final DtoFactory dtoFactory;

    public DtoProjectBridgeheadService(ProjectBridgeheadService projectBridgeheadService, DtoFactory dtoFactory) {
        this.projectBridgeheadService = projectBridgeheadService;
        this.dtoFactory = dtoFactory;
    }

    public List<de.samply.frontend.dto.ProjectBridgehead> fetchUserVisibleProjectBridgeheads(@NotNull Project project) throws ProjectBridgeheadServiceException {
        Set<ProjectBridgehead> projectBridgeheads = projectBridgeheadService.fetchBridgeheads(project);
        if (projectBridgeheadService.isProjectManagerAdmin()) {
            return new ArrayList<>(projectBridgeheads).stream().map(dtoFactory::convert).toList();
        }
        Set<ProjectBridgehead> tempProjectBridgeheads = new HashSet<>();
        projectBridgeheads.forEach(projectBridgehead -> {
            if (projectBridgeheadService.isBridgeheadAdminOfProjectBridgehead(projectBridgehead) ||
                    projectBridgeheadService.isUserOfProjectBridgehead(projectBridgehead) ||
                    projectBridgeheadService.isUserCreatorOfProject(project)
            ) {
                tempProjectBridgeheads.add(projectBridgehead);
            }
        });
        return new ArrayList<>(tempProjectBridgeheads).stream().map(dtoFactory::convert).toList();
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
