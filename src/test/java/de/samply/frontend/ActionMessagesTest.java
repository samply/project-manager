package de.samply.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.security.SessionUser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ActionMessagesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SessionUser sessionUser = mock(SessionUser.class);

    @Test
    void resolvesLocalizedMessagesFromNewSchema() throws Exception {
        ActionMessages messages = objectMapper.readValue("""
                {
                  "messages": {
                    "CREATE_PROJECT": [
                      {
                        "priority": 100,
                        "explanation": {"EN": "Complete the project."},
                        "success-message": {"EN": "Project submitted."},
                        "error-message": {"EN": "Project submission failed."}
                      }
                    ]
                  }
                }
                """, ActionMessages.class);

        Optional<ResolvedActionMessages> resolved = messages.fetchMessages(
                "CREATE_PROJECT", "PROJECT_STATE", "en",
                Optional.empty(), Optional.empty(), Optional.empty(), sessionUser);

        assertThat(resolved).contains(new ResolvedActionMessages(
                "Complete the project.",
                "Project submitted.",
                "Project submission failed.",
                100
        ));
    }

    @Test
    void selectsAllMessagesFromTheSameApplicableEntry() throws Exception {
        ActionMessages messages = objectMapper.readValue("""
                {
                  "messages": {
                    "CREATE_PROJECT": [
                      {
                        "module": "OTHER_MODULE",
                        "explanation": {"en": "Wrong explanation."},
                        "success-message": {"en": "Wrong success."}
                      },
                      {
                        "module": "PROJECT_STATE",
                        "explanation": {"en": "Right explanation."},
                        "success-message": {"en": "Right success."}
                      }
                    ]
                  }
                }
                """, ActionMessages.class);

        Optional<ResolvedActionMessages> resolved = messages.fetchMessages(
                "CREATE_PROJECT", "PROJECT_STATE", "en",
                Optional.empty(), Optional.empty(), Optional.empty(), sessionUser);

        assertThat(resolved).contains(new ResolvedActionMessages(
                "Right explanation.", "Right success.", null, 0
        ));
    }

    @Test
    void resolvesEntryContainingOnlyOutcomeMessages() throws Exception {
        ActionMessages messages = objectMapper.readValue("""
                {
                  "messages": {
                    "CREATE_PROJECT": [
                      {
                        "success-message": {"en": "Project submitted."},
                        "error-message": {"en": "Project submission failed."}
                      }
                    ]
                  }
                }
                """, ActionMessages.class);

        Optional<ResolvedActionMessages> resolved = messages.fetchMessages(
                "CREATE_PROJECT", "PROJECT_STATE", "en",
                Optional.empty(), Optional.empty(), Optional.empty(), sessionUser);

        assertThat(resolved).contains(new ResolvedActionMessages(
                null, "Project submitted.", "Project submission failed.", 0
        ));
    }

    @Test
    void actionResponseUsesCamelCaseMessageProperties() throws Exception {
        Action action = new Action(
                "/project", "POST", new String[]{"project-code"},
                "Complete the project.", "Project submitted.", "Project submission failed.", 100
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(action));

        assertThat(json.get("successMessage").textValue()).isEqualTo("Project submitted.");
        assertThat(json.get("errorMessage").textValue()).isEqualTo("Project submission failed.");
        assertThat(json.has("success-message")).isFalse();
        assertThat(json.has("error-message")).isFalse();
    }
}
