package de.samply.form.core.history;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.form.core.history.FormDefinitionConflict.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FormDefinitionFixTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String RECORDED = """
            [{"title": "ethics", "fields": [
              {"label": "status", "data_type": "STRING", "display_name": {"en": "Status"}},
              {"label": "vote", "data_type": "STRING"}
            ]}]""";

    @Test
    void aChangedFieldIsKeptInactiveAndItsNewDefinitionGetsTheNextLabel() throws Exception {
        String current = """
                [{"title": "ethics", "fields": [
                  {"label": "status", "data_type": "ENUM", "display_name": {"en": "Status"},
                   "allowed_values": [{"label": "approved"}]},
                  {"label": "vote", "data_type": "STRING"}
                ]}]""";

        String explanation = FormDefinitionFix.explain("ethics", 3, tree(RECORDED), tree(current), List.of(
                new FormDefinitionConflict("ethics", "status", Kind.DATA_TYPE_CHANGED, "STRING", "ENUM")));

        assertThat(explanation)
                .contains("Incompatible change in form \"ethics\" (recorded version 3):")
                .contains("- Form \"ethics\", field \"status\": data_type changed: STRING -> ENUM")
                .contains("Field \"status\":");
        String snippet = explanation.substring(explanation.indexOf("Field \"status\":"));
        // The recorded field, inactive, then the current one under the next label.
        assertThat(snippet.indexOf("\"data_type\" : \"STRING\"")).isPositive()
                .isLessThan(snippet.indexOf("\"active\" : false"));
        assertThat(snippet.indexOf("\"active\" : false")).isLessThan(snippet.indexOf("\"label\" : \"status-v2\""));
        assertThat(snippet.indexOf("\"label\" : \"status-v2\"")).isLessThan(snippet.indexOf("\"data_type\" : \"ENUM\""));
        assertThat(snippet).doesNotContain("\"vote\"");
        assertThat(explanation).contains("Then update what still refers to \"status\": conditions");
    }

    @Test
    void aRemovedFieldIsOnlyAddedBackInactive() throws Exception {
        String current = """
                [{"title": "ethics", "fields": [{"label": "status", "data_type": "STRING"}]}]""";

        String explanation = FormDefinitionFix.explain("ethics", 1, tree(RECORDED), tree(current), List.of(
                new FormDefinitionConflict("ethics", "vote", Kind.FIELD_REMOVED, null, null)));

        String snippet = explanation.substring(explanation.indexOf("Field \"vote\":"));
        assertThat(snippet).contains("\"label\" : \"vote\"", "\"active\" : false").doesNotContain("-v2");
        assertThat(explanation).doesNotContain("Then update what still refers to");
    }

    @Test
    void aRemovedFormGetsItsRecordedDefinitionToRestore() throws Exception {
        String explanation = FormDefinitionFix.explain("ethics", 5, tree(RECORDED), null, List.of(
                new FormDefinitionConflict("ethics", null, Kind.FORM_REMOVED, null, null)));

        assertThat(explanation)
                .contains("- Form \"ethics\": form removed or renamed")
                .contains("Keep the form, marked inactive")
                .contains("\"title\" : \"ethics\"", "\"label\" : \"status\"", "\"label\" : \"vote\"")
                .contains("give the new form the title \"ethics-v2\"", "frontend-project-configs.json");
        // The form-level flag, after the form's fields (appended at the end of its object).
        assertThat(explanation.lastIndexOf("\"active\" : false"))
                .isGreaterThan(explanation.indexOf("\"label\" : \"vote\""));
    }

    @Test
    void theNextLabelCountsUpAndSkipsLabelsInUse() {
        assertThat(FormDefinitionFix.nextLabel("status", Set.of())).isEqualTo("status-v2");
        assertThat(FormDefinitionFix.nextLabel("status-v2", Set.of())).isEqualTo("status-v3");
        assertThat(FormDefinitionFix.nextLabel("status", Set.of("status-v2", "status-v3"))).isEqualTo("status-v4");
    }

    private static JsonNode tree(String json) throws Exception {
        return OBJECT_MAPPER.readTree(json);
    }
}
