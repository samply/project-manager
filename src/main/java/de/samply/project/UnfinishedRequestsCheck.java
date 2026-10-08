package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.repository.ProjectRepository;
import de.samply.modules.OptionalModules;
import de.samply.project.state.ProjectState;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Stops the start when requests that are not finished need a disabled optional module: they could neither go on nor
 * be closed in the normal way. The message lists them and says how to solve it: enable the modules again and close the
 * requests in the UI, or close them with SQL (drafts cannot be archived, they are rejected). Finished, rejected and
 * archived requests do not block: they can still be viewed.
 */
@Slf4j
@Component
public class UnfinishedRequestsCheck {

    static final Set<ProjectState> NOT_FINISHED = EnumSet.of(ProjectState.DRAFT, ProjectState.REVIEW,
            ProjectState.APPROVAL, ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL);

    private final ProjectRepository projectRepository;
    private final OptionalModules optionalModules;

    public UnfinishedRequestsCheck(ProjectRepository projectRepository, OptionalModules optionalModules) {
        this.projectRepository = projectRepository;
        this.optionalModules = optionalModules;
    }

    @PostConstruct
    void check() {
        List<ProjectType> unavailable = Arrays.stream(ProjectType.values())
                .filter(projectType -> !optionalModules.isAvailable(projectType))
                .toList();
        if (unavailable.isEmpty()) {
            return;
        }
        List<Project> projects = projectRepository.findByStateInAndProjectTypeIn(NOT_FINISHED, unavailable);
        if (!projects.isEmpty()) {
            String message = describe(projects);
            log.error(message);
            throw new IllegalStateException(message);
        }
    }

    String describe(List<Project> projects) {
        List<Project> sorted = projects.stream().sorted(Comparator.comparing(Project::getCode)).toList();
        String requests = sorted.stream()
                .map(project -> "  " + project.getCode() + " (" + project.getState() + "): " + project.fetchProjectTypes().stream()
                        .filter(projectType -> !optionalModules.isAvailable(projectType))
                        .sorted()
                        .map(optionalModules::describeUnavailable)
                        .collect(Collectors.joining("; ")))
                .collect(Collectors.joining("\n"));
        List<String> toArchive = codes(sorted, project -> project.getState() != ProjectState.DRAFT);
        List<String> toReject = codes(sorted, project -> project.getState() == ProjectState.DRAFT);
        return "The backend does not start: " + projects.size() + " request(s) not finished need disabled modules.\n"
                + requests + "\n"
                + "Either enable the modules again and archive or finish these requests in the UI, or close them with SQL:"
                + (toArchive.isEmpty() ? "" : "\n  UPDATE samply.project SET state = 'ARCHIVED', archived_at = now(), modified_at = now()"
                + " WHERE code IN (" + quoted(toArchive) + ");")
                + (toReject.isEmpty() ? "" : "\n  UPDATE samply.project SET state = 'REJECTED', modified_at = now()"
                + " WHERE code IN (" + quoted(toReject) + "); -- drafts cannot be archived");
    }

    private List<String> codes(List<Project> projects, Predicate<Project> filter) {
        return projects.stream().filter(filter).map(Project::getCode).toList();
    }

    private String quoted(List<String> codes) {
        return codes.stream().map(code -> "'" + code + "'").collect(Collectors.joining(", "));
    }

}
