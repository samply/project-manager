package de.samply.feasibility;

import com.fasterxml.jackson.databind.JsonNode;
import de.samply.annotations.ModuleStandIn;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.modules.OptionalModule;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Stand-in while the module FEASIBILITY is disabled: no result. The frontend asks first whether feasibility is enabled
 * and does not show the table otherwise.
 */
@Slf4j
@Service
@ModuleStandIn(OptionalModule.FEASIBILITY)
public class DisabledFeasibilityService implements FeasibilityService {

    @Override
    public Mono<JsonNode> fetchFeasibility(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) {
        log.debug("Feasibility is disabled: no result for project {} and bridgehead {}", project.getCode(),
                bridgehead.getBridgehead());
        return Mono.empty();
    }

}
