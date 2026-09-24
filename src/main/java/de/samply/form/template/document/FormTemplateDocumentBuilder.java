package de.samply.form.template.document;

import de.samply.form.template.document.FormFieldLayoutResolver.KeyedField;
import de.samply.form.template.document.FormTemplateDocument.BlockHeading;
import de.samply.form.template.document.FormTemplateDocument.CheckBoxList;
import de.samply.form.template.document.FormTemplateDocument.CheckBoxOption;
import de.samply.form.template.document.FormTemplateDocument.FieldRow;
import de.samply.form.template.document.FormTemplateDocument.GroupHeading;
import de.samply.form.template.document.FormTemplateDocument.Node;
import de.samply.form.template.document.FormTemplateDocument.SectionHeading;
import de.samply.form.template.document.FormTemplateFieldPlacement.PlacedField;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.FormFieldGroup;
import de.samply.frontend.dto.FormFieldAllowedValue;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.samply.frontend.dto.FormField.fullOrShort;

/**
 * Builds a {@link FormTemplateDocument} from the placed fields, in print order
 * (see {@link FormTemplateFieldPlacement}).
 * <p>
 * Walks the fields in order and, for each one, emits
 * <ol>
 *   <li>a section heading when its section differs from the previous field's
 *   (header-block fields, which have no section, come first and get none);</li>
 *   <li>group headings for every level of its groups from the first level
 *   that differs from the previous field's, as the frontend Summary does;</li>
 *   <li>a block heading when its block or block instance differs from the
 *   previous field's;</li>
 *   <li>its row: a checkbox list for a CHECK_BOX field (once, for all its
 *   values), a plain row, or the whole layout row if it is that row's first
 *   field. Other members of a layout row emit no row of their own, but still
 *   count as "the previous field" for the next headings.</li>
 * </ol>
 * A new section starts without a previous field, so its first field's groups
 * and block always get headings.
 */
public final class FormTemplateDocumentBuilder {

    private static final String CHECK_BOX = "CHECK_BOX";
    private static final String CSS_PROPERTY_PREFIX = "CSS_";
    // Relative widths of paired fields in a layout row, as in the frontend
    // (ProjectView.vue .css-enum / .css-enum-value, ProjectFieldRow.vue getWidth).
    private static final Map<String, Integer> LAYOUT_WEIGHTS = Map.of(
            "CSS_ENUM", 40, "CSS_ENUM_VALUE", 60, "CSS_UNIT_VALUE", 54, "CSS_UNIT", 30);
    // Share of the cell the layout-row fields take together (as before).
    private static final int LAYOUT_ROW_WIDTH = 96;

    private FormTemplateDocumentBuilder() {
    }

    public static FormTemplateDocument build(
            @NotNull List<PlacedField> placedFields,
            @NotNull Map<String, Form> formsByTitle,
            @NotNull FormFieldLayoutResolver layoutRows) {
        Map<String, Set<String>> checkedValues = fetchCheckedValues(placedFields);
        Set<String> listedCheckBoxes = new HashSet<>();
        List<Node> nodes = new ArrayList<>();
        String section = null;
        FormField previous = null;
        for (PlacedField placed : placedFields) {
            FormField field = placed.field();
            // A CHECK_BOX field arrives as one field per selected value: the
            // first one lists them all, the others add nothing.
            if (isCheckBox(field) && !listedCheckBoxes.add(checkBoxKey(field))) {
                continue;
            }

            if (!Objects.equals(placed.section(), section)) {
                section = placed.section();
                previous = null;
                if (section != null) {
                    Form form = formsByTitle.getOrDefault(section, new Form(section, null, null, null));
                    nodes.add(new SectionHeading(form,
                            fullOrShort(form.titleDescription(), form.titleShortDescription())));
                }
            }

            addGroupHeadings(nodes, field, previous);

            if (field.block() != null) {
                boolean blockChanged = previous == null || !Objects.equals(previous.block(), field.block());
                if (blockChanged || !Objects.equals(previous.blockInstance(), field.blockInstance())) {
                    nodes.add(new BlockHeading(
                            fetchBlockTitle(field),
                            blockChanged ? fullOrShort(field.blockDescription(), field.blockShortDescription()) : null));
                }
            }

            List<KeyedField> layoutRow = layoutRows.rowFieldsStartingAt(placed.key());
            if (isCheckBox(field)) {
                nodes.add(createCheckBoxList(placed, checkedValues.get(checkBoxKey(field))));
            } else if (layoutRow != null) {
                nodes.add(new FieldRow(layoutRow, true, fetchLayoutWidths(layoutRow), false));
            } else if (!layoutRows.isSkippedRowMember(placed.key())) {
                nodes.add(new FieldRow(List.of(new KeyedField(placed.key(), field)), false,
                        List.of(), isWithoutLabel(field)));
            }

            previous = field;
        }
        return new FormTemplateDocument(nodes);
    }

    // Once a level differs, every deeper level starts anew too - even if a
    // deeper group happens to have the same name under a different parent.
    private static void addGroupHeadings(List<Node> nodes, FormField field, FormField previous) {
        FormFieldGroup[] groups = groupsOf(field);
        FormFieldGroup[] previousGroups = previous != null ? groupsOf(previous) : new FormFieldGroup[0];
        int changedLevel = 0;
        while (changedLevel < groups.length && changedLevel < previousGroups.length
                && Objects.equals(groups[changedLevel].group(), previousGroups[changedLevel].group())) {
            changedLevel++;
        }
        for (int level = changedLevel; level < groups.length; level++) {
            FormFieldGroup group = groups[level];
            nodes.add(new GroupHeading(group, level + 1, fullOrShort(group.description(), group.shortDescription())));
        }
    }

    private static FormFieldGroup[] groupsOf(FormField field) {
        return field.groups() != null ? field.groups() : new FormFieldGroup[0];
    }

    // A repeatable block's instances are numbered, e.g. "Collaborator 2". A
    // missing display name or instance is left out, never printed as "null".
    private static String fetchBlockTitle(FormField field) {
        return Stream.of(field.blockDisplayName(),
                        Boolean.TRUE.equals(field.multipleBlock()) ? field.blockInstance() : null)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    /**
     * Each field's share of the row: proportional to the frontend's widths if
     * every field of the row has one (e.g. an enum and its free-text
     * continuation 40/60), else equal.
     */
    private static List<Integer> fetchLayoutWidths(List<KeyedField> row) {
        List<Integer> weights = row.stream().map(field -> fetchLayoutWeight(field.field())).toList();
        if (weights.contains(null)) {
            return row.stream().map(_ -> LAYOUT_ROW_WIDTH / row.size()).toList();
        }
        int total = weights.stream().mapToInt(Integer::intValue).sum();
        return weights.stream().map(weight -> LAYOUT_ROW_WIDTH * weight / total).toList();
    }

    private static Integer fetchLayoutWeight(FormField field) {
        return properties(field).stream().map(LAYOUT_WEIGHTS::get).filter(Objects::nonNull).findFirst().orElse(null);
    }

    // As in the frontend Summary: a field with neither display name nor
    // description takes the whole row - unless it is part of a CSS layout.
    private static boolean isWithoutLabel(FormField field) {
        return (field.labelDisplayName() == null || field.labelDisplayName().isBlank())
                && field.fetchFullLabelDescription() == null
                && properties(field).stream().noneMatch(property -> property.startsWith(CSS_PROPERTY_PREFIX));
    }

    private static boolean isCheckBox(FormField field) {
        return properties(field).contains(CHECK_BOX)
                && field.allowedValues() != null && field.allowedValues().length > 0;
    }

    private static List<String> properties(FormField field) {
        return field.properties() != null ? Arrays.asList(field.properties()) : List.of();
    }

    // The values of one CHECK_BOX field share title, label and block instance.
    private static String checkBoxKey(FormField field) {
        return field.title() + "|" + field.label() + "|" + Objects.toString(field.blockInstance(), "");
    }

    private static Map<String, Set<String>> fetchCheckedValues(List<PlacedField> placedFields) {
        Map<String, Set<String>> result = new HashMap<>();
        placedFields.stream()
                .map(PlacedField::field)
                .filter(FormTemplateDocumentBuilder::isCheckBox)
                .forEach(field -> {
                    Set<String> values = result.computeIfAbsent(checkBoxKey(field), _ -> new HashSet<>());
                    if (field.value() != null && !field.value().isBlank()) {
                        values.add(field.value());
                    }
                });
        return result;
    }

    private static CheckBoxList createCheckBoxList(PlacedField placed, Set<String> checkedValues) {
        // One list for all values: no "(n)" instance suffix on its label.
        FormField field = placed.field().toBuilder().fieldInstance(null).build();
        List<CheckBoxOption> options = Arrays.stream(field.allowedValues())
                .map(value -> createOption(placed.key(), value, checkedValues.contains(value.label())))
                .toList();
        return new CheckBoxList(new KeyedField(placed.key(), field), options, isWithoutLabel(field));
    }

    private static CheckBoxOption createOption(String fieldKey, FormFieldAllowedValue value, boolean checked) {
        return new CheckBoxOption(
                fieldKey + "_" + value.label(),
                value.displayName() != null ? value.displayName() : value.label(),
                FormField.fetchValueDescription(value),
                checked);
    }
}
