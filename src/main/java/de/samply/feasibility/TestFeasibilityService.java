package de.samply.feasibility;

import de.samply.annotations.ModuleTest;
import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.frontend.dto.FeasibilityItem;
import de.samply.modules.ModuleMode;
import de.samply.modules.OptionalModule;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Test mode of FEASIBILITY (ENABLE_FEASIBILITY=test), for development: a random result from the template
 * TEST_FEASIBILITY_RESULT (e.g. {{INTEGER}} replaced by a random number), without Beam and without asking the
 * bridgeheads. Mapped like a real answer.
 */
@Service
@ModuleTest(OptionalModule.FEASIBILITY)
public class TestFeasibilityService implements FeasibilityService {

    private final String testFeasibilityResult;
    private final TestFeasibilityResultResolver testFeasibilityResultResolver;
    private final FeasibilityMapper feasibilityMapper;

    public TestFeasibilityService(@Value(ProjectManagerConst.TEST_FEASIBILITY_RESULT_SV) String testFeasibilityResult,
                                  TestFeasibilityResultResolver testFeasibilityResultResolver,
                                  FeasibilityMapper feasibilityMapper) {
        if (testFeasibilityResult == null || testFeasibilityResult.isBlank()) {
            throw new IllegalStateException(ProjectManagerConst.ENABLE_FEASIBILITY + "=" + ModuleMode.TEST + " needs "
                    + ProjectManagerConst.TEST_FEASIBILITY_RESULT);
        }
        this.testFeasibilityResult = testFeasibilityResult;
        this.testFeasibilityResultResolver = testFeasibilityResultResolver;
        this.feasibilityMapper = feasibilityMapper;
    }

    @Override
    public Mono<List<FeasibilityItem>> fetchFeasibility(@NotNull Project project, @NotNull ProjectBridgehead bridgehead) {
        return Mono.fromCallable(() -> testFeasibilityResultResolver.resolve(testFeasibilityResult))
                .map(feasibilityMapper::map);
    }

}
