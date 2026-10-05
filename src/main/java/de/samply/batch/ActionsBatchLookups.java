package de.samply.batch;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The projects and bridgeheads the entries of one actions batch resolve from their project-code and bridgehead
 * parameters: most entries of a batch name the same ones, so each is loaded from the database once per batch instead of
 * once per entry. Lives as long as its batch; nothing is kept across requests.
 * <p>
 * The entries share the loaded entity objects. That is safe because a batch only runs read actions (GET), which do not
 * change them (see docs/actions-batch-concurrency.md).
 */
public class ActionsBatchLookups {

    // Every entry request of a batch carries the lookups of its batch under this attribute
    static final String REQUEST_ATTRIBUTE = ActionsBatchLookups.class.getName();

    private final Map<String, Project> projects = new ConcurrentHashMap<>();
    private final Map<BridgeheadKey, ProjectBridgehead> bridgeheads = new ConcurrentHashMap<>();

    /** The lookups of the batch the current request belongs to; empty outside a batch. */
    public static Optional<ActionsBatchLookups> current() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(attributes -> attributes.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .map(ActionsBatchLookups.class::cast);
    }

    // Entries asking at the same time wait for one load. A load that fails (exception or null) is not kept.
    public Project project(String projectCode, Supplier<Project> load) {
        return projects.computeIfAbsent(projectCode, code -> load.get());
    }

    public ProjectBridgehead bridgehead(String projectCode, String bridgehead, Supplier<ProjectBridgehead> load) {
        return bridgeheads.computeIfAbsent(new BridgeheadKey(projectCode, bridgehead), key -> load.get());
    }

    // A bridgehead of one project: the same bridgehead id is part of many projects
    private record BridgeheadKey(String projectCode, String bridgehead) {
    }

}
