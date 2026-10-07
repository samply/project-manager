package de.samply.feasibility;

import com.fasterxml.jackson.databind.JsonNode;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

/**
 * Feasibility of a request at one bridgehead: how many patients, samples, ... its query finds there. Part of the
 * optional module FEASIBILITY: {@link BeamFeasibilityService} when enabled, {@link DisabledFeasibilityService} when not.
 */
public interface FeasibilityService {

    /** The bridgehead's raw answer (mapped for the frontend by {@link FeasibilityMapper}); empty when there is none. */
    Mono<JsonNode> fetchFeasibility(@NotNull Project project, @NotNull ProjectBridgehead bridgehead);

}
