package de.samply.feasibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.frontend.dto.FeasibilityItem;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestFeasibilityServiceTest {

    private static final String TEMPLATE = "[{\"label\": \"Patients\", \"value\": {{INTEGER}}}, "
            + "{\"label\": \"Samples\", \"value\": {{INTEGER}}}]";

    @Test
    void returnsTheResolvedTemplateMapped() throws Exception {
        // As in development: the template already has the final form, so the mapping is the identity
        FeasibilityMapper identityMapper = new FeasibilityMapper(".");
        identityMapper.init();
        TestFeasibilityService service = new TestFeasibilityService(TEMPLATE,
                new TestFeasibilityResultResolver(new ObjectMapper(), new Random(42)), identityMapper);

        StepVerifier.create(service.fetchFeasibility(new Project(), new ProjectBridgehead()))
                .assertNext(result -> {
                    assertThat(result).extracting(FeasibilityItem::label).containsExactly("Patients", "Samples");
                    assertThat(result.get(0).value()).isNotEqualTo(result.get(1).value());
                })
                .verifyComplete();
    }

    @Test
    void needsATemplate() {
        assertThatThrownBy(() -> new TestFeasibilityService(" ", new TestFeasibilityResultResolver(), new FeasibilityMapper(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ENABLE_FEASIBILITY=test needs TEST_FEASIBILITY_RESULT");
    }

}
