package de.samply.form.template;

import de.samply.form.template.FormTemplateFieldPlacement.PlacedField;
import de.samply.form.template.FormTemplateFieldPlacement.ProjectFieldPlacement;
import de.samply.frontend.dto.FormField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FormTemplateFieldPlacementTest {

    @Test
    void ordersSectionsAndPutsUnplacedProjectFieldsInTheHeader() {
        List<PlacedField> placed = FormTemplateFieldPlacement.place(
                List.of("query", "project"),
                List.of(project("creator", null, null), project("email", null, null)),
                List.of(dynamic("project", "title", 1), dynamic("query", "cohort", 1)));

        assertThat(describe(placed)).containsExactly(
                "- creator", "- email", "query cohort", "project title");
    }

    @Test
    void aProjectFieldForASectionThatIsNotPrintedGoesToTheHeader() {
        List<PlacedField> placed = FormTemplateFieldPlacement.place(
                List.of("project"),
                List.of(project("sites", "query", null)),
                List.of(dynamic("project", "title", 1)));

        assertThat(describe(placed)).containsExactly("- sites", "project title");
    }

    @Test
    void mergesProjectFieldsThatFollowAFixedEntryByOrderAndPutsTheOthersFirst() {
        List<PlacedField> placed = FormTemplateFieldPlacement.place(
                List.of("project"),
                List.of(project("description", "project", null),   // KEEP_FIXED_FIELD_ORDER / form_title
                        project("title", "project", 2)),             // follows its FIXED entry's order
                List.of(dynamic("project", "acronym", 3), dynamic("project", "field", 1)));

        assertThat(describe(placed)).containsExactly(
                "project description", "project field", "project title", "project acronym");
    }

    @Test
    void keepsEachBlockInstanceTogetherAtTheBlocksLowestOrder() {
        List<PlacedField> placed = FormTemplateFieldPlacement.place(
                List.of("project"),
                List.of(),
                List.of(dynamic("project", "summary", 5),
                        block("name", 1, 2), block("email", 1, 3),
                        block("name", 2, 2), block("email", 2, 3),
                        dynamic("project", "title", 1)));

        assertThat(describe(placed)).containsExactly(
                "project title", "project name#1", "project email#1", "project name#2", "project email#2",
                "project summary");
    }

    @Test
    void fieldsWithoutOrderComeLastInTheirSourceOrder() {
        List<PlacedField> placed = FormTemplateFieldPlacement.place(
                List.of("project"),
                List.of(),
                List.of(dynamic("project", "b", null), dynamic("project", "a", null), dynamic("project", "c", 7)));

        assertThat(describe(placed)).containsExactly("project c", "project b", "project a");
    }

    private static ProjectFieldPlacement project(String label, String section, Integer order) {
        return new ProjectFieldPlacement(
                FormField.builder().title("project-fields-request").label(label).build(), section, order);
    }

    private static FormField dynamic(String title, String label, Integer order) {
        return FormField.builder().title(title).label(label).order(order).build();
    }

    private static FormField block(String label, int instance, int order) {
        return FormField.builder().title("project").label(label).order(order)
                .block("collaborators").blockInstance(instance).multipleBlock(true).build();
    }

    private static List<String> describe(List<PlacedField> placed) {
        return placed.stream()
                .map(field -> (field.section() == null ? "-" : field.section()) + " " + field.field().label()
                        + (field.field().blockInstance() != null ? "#" + field.field().blockInstance() : ""))
                .toList();
    }
}
