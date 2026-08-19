package de.samply.frontend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.UserProjectState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BridgeheadDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void projectBridgeheadSerializesInheritedBridgeheadContactData() throws Exception {
        BridgeheadContact contact = new BridgeheadContact(
                "Master team",
                "English description",
                "master@example.org");
        ProjectBridgehead projectBridgehead = new ProjectBridgehead(
                "project-1",
                "master",
                "MASTER",
                new BridgeheadContact[]{contact},
                ProjectBridgeheadState.ACCEPTED,
                Instant.parse("2026-01-01T00:00:00Z"),
                new ProjectBridgeheadExecution[0],
                UserProjectState.ACCEPTED);

        String json = objectMapper.writeValueAsString(projectBridgehead);

        assertThat(json).contains("\"bridgehead\":\"master\"");
        assertThat(json).contains("\"humanReadable\":\"MASTER\"");
        assertThat(json).contains("\"contacts\"");
        assertThat(json).contains("\"name\":\"Master team\"");
        assertThat(json).contains("\"description\":\"English description\"");
        assertThat(json).contains("\"emailAddress\":\"master@example.org\"");
    }
}
