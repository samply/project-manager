package de.samply.form.core.history;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.form.core.history.FormDefinitionConflict.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FormDefinitionComparatorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String RECORDED = """
            [{
              "title": "project",
              "blocks": [{"label": "collaborators", "multiple": true}, {"label": "lead", "multiple": false}],
              "fields": [
                {"label": "title", "data_type": "STRING", "display_name": {"en": "Title"}, "mandatory": true},
                {"label": "status", "data_type": "ENUM", "allowed_values": [{"label": "approved"}, {"label": "pending"}]},
                {"label": "name", "data_type": "STRING", "block": "collaborators"},
                {"label": "lead_name", "data_type": "STRING", "block": "lead"},
                {"label": "comments", "data_type": "STRING", "multiple": true},
                {"label": "tags", "data_type": "STRING"},
                {"label": "upload", "data_type": "STRING", "as_file": true},
                {"label": "PROJECT_TITLE", "field_type": "FIXED", "data_type": "STRING"}
              ]
            }]
            """;

    @Test
    void anUnchangedDefinitionHasNoConflicts() {
        assertThat(compare(RECORDED, RECORDED)).isEmpty();
    }

    @Test
    void textsOrderAndOtherUnprotectedAttributesMayChangeFreely() {
        String current = RECORDED
                .replace("\"display_name\": {\"en\": \"Title\"}, \"mandatory\": true",
                        "\"display_name\": {\"en\": \"Project title\"}, \"mandatory\": false, \"active\": false,"
                                + " \"properties\": [\"CSS_UNIT\"], \"condition\": \"true\"")
                .replace("{\"label\": \"approved\"}, {\"label\": \"pending\"}",
                        "{\"label\": \"pending\"}, {\"label\": \"approved\", \"display_name\": {\"en\": \"OK\"}}");

        assertThat(compare(RECORDED, current)).isEmpty();
    }

    @Test
    void compatibleChangesHaveNoConflicts() {
        String current = RECORDED
                .replace("{\"label\": \"pending\"}]", "{\"label\": \"pending\"}, {\"label\": \"rejected\"}]")
                .replace("{\"label\": \"tags\", \"data_type\": \"STRING\"}",
                        "{\"label\": \"tags\", \"data_type\": \"STRING\", \"multiple\": true}")
                .replace("{\"label\": \"lead\", \"multiple\": false}", "{\"label\": \"lead\", \"multiple\": true}")
                .replace("{\"label\": \"title\", \"data_type\": \"STRING\"", "{\"label\": \"title\", \"data_type\": \"LONG_STRING\"")
                .replace("\"fields\": [", "\"fields\": [{\"label\": \"new_field\", \"data_type\": \"INTEGER\"},");

        assertThat(compare(RECORDED, current)).isEmpty();
    }

    @Test
    void reportsEveryIncompatibleChange() {
        String current = RECORDED
                .replace("{\"label\": \"title\", \"data_type\": \"STRING\"", "{\"label\": \"title\", \"data_type\": \"INTEGER\"")
                .replace("{\"label\": \"approved\"}, {\"label\": \"pending\"}", "{\"label\": \"accepted\"}, {\"label\": \"pending\"}")
                .replace("\"block\": \"collaborators\"", "\"block\": \"lead\"")
                .replace("{\"label\": \"comments\", \"data_type\": \"STRING\", \"multiple\": true}",
                        "{\"label\": \"comments\", \"data_type\": \"STRING\"}")
                .replace("\"as_file\": true", "\"as_file\": false")
                .replace("{\"label\": \"tags\", \"data_type\": \"STRING\"},", "")
                .replace("\"field_type\": \"FIXED\"", "\"field_type\": \"DYNAMIC\"");

        assertThat(compare(RECORDED, current)).extracting(FormDefinitionConflict::describe).containsExactly(
                "Form \"project\", field \"title\": data_type changed: STRING -> INTEGER",
                "Form \"project\", field \"status\": allowed values removed or renamed: approved",
                "Form \"project\", field \"name\": block changed: collaborators -> lead",
                "Form \"project\", field \"comments\": multiple changed from true to false: true -> false",
                "Form \"project\", field \"tags\": field removed",
                "Form \"project\", field \"upload\": as_file changed: true -> false",
                "Form \"project\", field \"PROJECT_TITLE\": field_type changed: FIXED -> DYNAMIC");
    }

    @Test
    void movingAFieldIntoOrOutOfABlockAndDisablingARepeatableBlockConflict() {
        String current = RECORDED
                .replace("{\"label\": \"tags\", \"data_type\": \"STRING\"}",
                        "{\"label\": \"tags\", \"data_type\": \"STRING\", \"block\": \"lead\"}")
                .replace("{\"label\": \"collaborators\", \"multiple\": true}", "{\"label\": \"collaborators\", \"multiple\": false}");

        assertThat(compare(RECORDED, current)).extracting(FormDefinitionConflict::describe).containsExactly(
                "Form \"project\", field \"name\": block multiple changed from true to false: collaborators",
                "Form \"project\", field \"tags\": block changed: none -> lead");
    }

    @Test
    void aFixedFieldOnlyConflictsWhenItStopsBeingFixed() {
        String current = RECORDED.replace(
                "{\"label\": \"PROJECT_TITLE\", \"field_type\": \"FIXED\", \"data_type\": \"STRING\"}",
                "{\"label\": \"PROJECT_TITLE\", \"field_type\": \"FIXED\", \"data_type\": \"LONG_STRING\", \"multiple\": false}");

        assertThat(compare(RECORDED, current)).isEmpty();
    }

    @Test
    void aDefinitionRecordedWithUnknownKeysStaysComparable() {
        String recorded = RECORDED.replace("\"title\": \"project\",", "\"title\": \"project\", \"legacy_key\": 1,");

        assertThat(compare(recorded, RECORDED)).isEmpty();
    }

    @Test
    void fieldsOfSeveralFilesWithTheSameTitleAreCompared() {
        String recorded = "[{\"title\": \"ethics\", \"fields\": []}, {\"title\": \"ethics\", \"fields\": [{\"label\": \"vote\", \"data_type\": \"STRING\"}]}]";
        String current = "[{\"title\": \"ethics\", \"fields\": []}]";

        assertThat(compare(recorded, current)).extracting(FormDefinitionConflict::kind).containsExactly(Kind.FIELD_REMOVED);
    }

    @Test
    void aDefinitionRecordedAsAnArrayOfFilesComparesWithTheFormObject() {
        String form = RECORDED.strip();
        form = form.substring(1, form.length() - 1);   // the form object, without the array around it

        assertThat(compare(RECORDED, form)).isEmpty();
    }

    private static List<FormDefinitionConflict> compare(String recorded, String current) {
        try {
            JsonNode before = OBJECT_MAPPER.readTree(recorded);
            JsonNode after = OBJECT_MAPPER.readTree(current);
            String title = FormDefinitionFactory.files(before).getFirst().path("title").asText();
            return FormDefinitionComparator.compare(title, before, after);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
