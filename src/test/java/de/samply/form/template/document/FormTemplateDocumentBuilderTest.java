package de.samply.form.template.document;

import de.samply.form.core.model.FormFieldLayoutRow;
import de.samply.form.template.document.FormFieldLayoutResolver.KeyedField;
import de.samply.form.template.document.FormTemplateDocument.BlockHeading;
import de.samply.form.template.document.FormTemplateDocument.CheckBoxList;
import de.samply.form.template.document.FormTemplateDocument.FieldRow;
import de.samply.form.template.document.FormTemplateDocument.GroupHeading;
import de.samply.form.template.document.FormTemplateDocument.Node;
import de.samply.form.template.document.FormTemplateDocument.SectionHeading;
import de.samply.form.template.document.FormTemplateFieldPlacement.PlacedField;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.FormFieldGroup;
import de.samply.frontend.dto.FormFieldAllowedValue;
import de.samply.utils.FormFieldUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FormTemplateDocumentBuilderTest {

    private static final FormFieldGroup GROUP_A = new FormFieldGroup("a", "Group A", null, null);
    private static final FormFieldGroup GROUP_B = new FormFieldGroup("b", "Group B", null, null);
    private static final FormFieldGroup GROUP_C = new FormFieldGroup("c", "Group C", null, null);
    private static final Map<String, Form> FORMS = Map.of(
            "project", new Form("project", "Project", "About the project", null),
            "ethics", new Form("ethics", "Ethics", null, null));

    @Test
    void headerFieldsComeFirstWithoutHeadingThenOneHeadingPerSection() {
        List<Node> nodes = build(Map.of(),
                placed(field("pf", "creator").build(), null),
                placed(field("project", "title").build(), "project"),
                placed(field("project", "summary").build(), "project"),
                placed(field("ethics", "vote").build(), "ethics"));

        assertThat(describe(nodes)).containsExactly(
                "row creator", "section Project", "row title", "row summary", "section Ethics", "row vote");
    }

    @Test
    void aSectionWithoutFormMetadataIsHeadedByItsTitle() {
        List<Node> nodes = build(Map.of(), placed(field("funding", "budget").build(), "funding"));

        assertThat(describe(nodes)).containsExactly("section funding", "row budget");
    }

    @Test
    void emitsGroupHeadingsForEveryLevelFromTheFirstThatChanged() {
        List<Node> nodes = build(Map.of(),
                placed(field("project", "one").groups(groups(GROUP_A)).build(), "project"),
                placed(field("project", "two").groups(groups(GROUP_A, GROUP_B)).build(), "project"),
                placed(field("project", "three").groups(groups(GROUP_A, GROUP_B)).build(), "project"),
                placed(field("project", "four").groups(groups(GROUP_C, GROUP_B)).build(), "project"),
                placed(field("project", "five").build(), "project"),
                placed(field("project", "six").groups(groups(GROUP_C)).build(), "project"));

        assertThat(describe(nodes)).containsExactly(
                "section Project",
                "group 1 Group A", "row one",
                "group 2 Group B", "row two", "row three",
                // Level 1 changed, so level 2 starts anew although it is also "b".
                "group 1 Group C", "group 2 Group B", "row four",
                "row five",
                "group 1 Group C", "row six");
    }

    @Test
    void aNewSectionRepeatsTheHeadingsOfItsFirstField() {
        List<Node> nodes = build(Map.of(),
                placed(field("project", "one").groups(groups(GROUP_A)).build(), "project"),
                placed(field("ethics", "two").groups(groups(GROUP_A)).build(), "ethics"));

        assertThat(describe(nodes)).containsExactly(
                "section Project", "group 1 Group A", "row one",
                "section Ethics", "group 1 Group A", "row two");
    }

    @Test
    void emitsABlockHeadingPerInstanceWithTheDescriptionOnlyOnce() {
        List<Node> nodes = build(Map.of(),
                collaborator(1, "name"), collaborator(1, "email"),
                collaborator(2, "name"), collaborator(2, "email"),
                placed(field("project", "budget").block("funding").blockDisplayName("Funding").blockInstance(1)
                        .multipleBlock(false).blockDescription("How it is funded").build(), "project"));

        assertThat(describe(nodes)).containsExactly(
                "section Project",
                "block Collaborator 1 | Each collaborator", "row name", "row email",
                "block Collaborator 2 | null", "row name", "row email",
                "block Funding | How it is funded", "row budget");
    }

    @Test
    void placesTheFieldsOfALayoutRowInOneRow() {
        List<Node> nodes = build(
                Map.of("s", List.of(new FormFieldLayoutRow(List.of("volume", "unit")))),
                placed(field("s", "type").build(), "s"),
                placed(field("s", "volume").build(), "s"),
                placed(field("s", "unit").build(), "s"),
                placed(field("s", "note").build(), "s"));

        assertThat(describe(nodes)).containsExactly(
                "section s", "row type", "layout volume+unit [48, 48]", "row note");
    }

    @Test
    void keepsSeparateRowsWhenALayoutRowMemberIsMissing() {
        List<Node> nodes = build(
                Map.of("s", List.of(new FormFieldLayoutRow(List.of("volume", "unit")))),
                placed(field("s", "volume").build(), "s"));

        assertThat(describe(nodes)).containsExactly("section s", "row volume");
    }

    @Test
    void keysEachRowFieldLikeTheFieldsMap() {
        FormField comment = field("project", "comment").multiple(true).fieldInstance(2).build();

        List<Node> nodes = build(Map.of(), placed(comment, "project"));

        assertThat(((FieldRow) nodes.get(1)).fields())
                .containsExactly(new KeyedField(FormFieldUtils.fetchFormFieldKey(comment), comment));
    }

    @Test
    void usesTheFullDescriptionsAndTheShortOnesOnlyAsFallback() {
        Map<String, Form> forms = Map.of(
                "full", new Form("full", "Full", "Full form text", "Short form text"),
                "short", new Form("short", "Short", " ", "Short form text"));
        FormFieldGroup group = new FormFieldGroup("g", "Group", null, "Short group text");
        FormField blockField = field("full", "name").block("b").blockDisplayName("Block").blockInstance(1)
                .multipleBlock(false).blockDescription("Full block text").blockShortDescription("Short block text")
                .groups(groups(group)).build();
        List<PlacedField> placed = List.of(placed(blockField, "full"), placed(field("short", "x").build(), "short"));

        List<Node> nodes = FormTemplateDocumentBuilder.build(placed, forms,
                FormFieldLayoutResolver.resolve(new LinkedHashMap<>(), Map.of())).nodes();

        assertThat(nodes).satisfiesExactly(
                node -> assertThat(((SectionHeading) node).description()).isEqualTo("Full form text"),
                node -> assertThat(((GroupHeading) node).description()).isEqualTo("Short group text"),
                node -> assertThat(((BlockHeading) node).description()).isEqualTo("Full block text"),
                node -> assertThat(node).isInstanceOf(FieldRow.class),
                node -> assertThat(((SectionHeading) node).description()).isEqualTo("Short form text"),
                node -> assertThat(node).isInstanceOf(FieldRow.class));
    }

    @Test
    void sharesALayoutRowLikeTheFrontendsCssProperties() {
        Map<String, List<FormFieldLayoutRow>> layouts = Map.of("s", List.of(
                new FormFieldLayoutRow(List.of("conflict", "conflict_text")),
                new FormFieldLayoutRow(List.of("volume", "unit"))));

        List<Node> nodes = build(layouts,
                placed(field("s", "conflict").properties(new String[]{"CSS_ENUM"}).build(), "s"),
                placed(field("s", "conflict_text").properties(new String[]{"CSS_ENUM_VALUE"}).build(), "s"),
                placed(field("s", "volume").properties(new String[]{"CSS_UNIT_VALUE"}).build(), "s"),
                placed(field("s", "unit").properties(new String[]{"CSS_UNIT"}).build(), "s"));

        assertThat(describe(nodes)).containsExactly(
                "section s", "layout conflict+conflict_text [38, 57]", "layout volume+unit [61, 34]");
    }

    @Test
    void aFieldWithoutDisplayNameAndDescriptionTakesTheWholeRow() {
        List<Node> nodes = build(Map.of(),
                placed(field("p", "vote").labelDisplayName(" ").build(), "p"),
                placed(field("p", "described").labelDisplayName(null).labelShortDescription("Upload the vote").build(), "p"),
                placed(field("p", "styled").properties(new String[]{"CSS_UNIT"}).build(), "p"),
                placed(field("p", "named").labelDisplayName("Named").build(), "p"));

        assertThat(describe(nodes)).containsExactly(
                "section p", "full vote", "row described", "row styled", "row named");
    }

    @Test
    void listsEveryOptionOfACheckBoxFieldOnceWithItsCheckedState() {
        FormFieldAllowedValue[] options = {
                new FormFieldAllowedValue("accurate", "Information is accurate", null, null),
                new FormFieldAllowedValue("purpose", "Data used only for this purpose", "Full text", "Short text")};
        FormField.FormFieldBuilder declaration = field("declaration", "confirmation").labelDisplayName(null).multiple(true)
                .properties(new String[]{"CHECK_BOX"}).allowedValues(options);

        List<Node> nodes = build(Map.of(),
                placed(declaration.fieldInstance(1).value("purpose").build(), "declaration"),
                placed(declaration.fieldInstance(2).value("").build(), "declaration"),
                placed(field("declaration", "consent").labelDisplayName("Consent").multiple(true)
                        .properties(new String[]{"CHECK_BOX"}).allowedValues(options).build(), "declaration"));

        assertThat(describe(nodes)).containsExactly(
                "section declaration",
                "full-checkboxes confirmation [ ]Information is accurate,[x]Data used only for this purpose",
                "checkboxes consent [ ]Information is accurate,[ ]Data used only for this purpose");
        CheckBoxList list = (CheckBoxList) nodes.get(1);
        assertThat(list.field().field().fieldInstance()).isNull();
        assertThat(list.options().get(1).description()).isEqualTo("Full text");
        assertThat(list.options()).extracting(FormTemplateDocument.CheckBoxOption::name)
                .doesNotHaveDuplicates();
    }

    @Test
    void neverPrintsNullInABlockTitle() {
        List<Node> nodes = build(Map.of(),
                placed(field("p", "name").block("collaborators").blockDisplayName("Collaborator")
                        .multipleBlock(true).build(), "p"),
                placed(field("p", "site").block("sites").blockInstance(2).multipleBlock(true).build(), "p"));

        assertThat(describe(nodes)).containsExactly(
                "section p", "block Collaborator | null", "row name", "block 2 | null", "row site");
    }

    private static PlacedField collaborator(int instance, String label) {
        return placed(field("project", label).block("collaborators").blockDisplayName("Collaborator")
                .blockDescription("Each collaborator").blockInstance(instance).multipleBlock(true).build(), "project");
    }

    private static FormFieldGroup[] groups(FormFieldGroup... groups) {
        return groups;
    }

    private static FormField.FormFieldBuilder field(String title, String label) {
        // Named by default: a field without display name and description takes the whole row.
        return FormField.builder().title(title).label(label).labelDisplayName(label);
    }

    private static PlacedField placed(FormField field, String section) {
        return new PlacedField(FormFieldUtils.fetchFormFieldKey(field), field, section);
    }

    private static List<Node> build(Map<String, List<FormFieldLayoutRow>> layouts, PlacedField... placedFields) {
        Map<String, FormField> byKey = new LinkedHashMap<>();
        for (PlacedField placed : placedFields) {
            byKey.put(placed.key(), placed.field());
        }
        return FormTemplateDocumentBuilder.build(new ArrayList<>(List.of(placedFields)), FORMS,
                FormFieldLayoutResolver.resolve(byKey, layouts)).nodes();
    }

    private static List<String> describe(List<Node> nodes) {
        return nodes.stream().map(node -> switch (node) {
            case SectionHeading heading -> "section " + (heading.form().titleDisplayName() != null
                    ? heading.form().titleDisplayName() : heading.form().title());
            case GroupHeading heading -> "group " + heading.level() + " " + heading.group().displayName();
            case BlockHeading heading -> "block " + heading.title() + " | " + heading.description();
            case FieldRow row when row.layout() -> "layout " + String.join("+",
                    row.fields().stream().map(field -> field.field().label()).toList()) + " " + row.widths();
            case FieldRow row -> (row.fullWidth() ? "full " : "row ") + row.fields().getFirst().field().label();
            case CheckBoxList list -> (list.fullWidth() ? "full-checkboxes " : "checkboxes ")
                    + list.field().field().label() + " " + String.join(",", list.options().stream()
                    .map(option -> (option.checked() ? "[x]" : "[ ]") + option.displayName()).toList());
        }).toList();
    }
}
