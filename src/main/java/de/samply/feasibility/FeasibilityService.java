package de.samply.feasibility;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.frontend.dto.FeasibilityItem;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Feasibility of a request at one bridgehead: how many patients, samples, ... its query finds there. Part of the
 * optional module FEASIBILITY: {@link BeamFeasibilityService} when enabled, {@link DisabledFeasibilityService} when not.
 */
public interface FeasibilityService {

    /** The bridgehead's answer, mapped for the frontend ({@link FeasibilityMapper}); empty when there is none. */
    Mono<List<FeasibilityItem>> fetchFeasibility(@NotNull Project project, @NotNull ProjectBridgehead bridgehead);

}
