package de.samply.form.core.history;

import com.fasterxml.jackson.databind.JsonNode;
import de.samply.app.ProjectManagerConst;
import de.samply.form.core.history.FormDefinitionConflict.Kind;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compares a form's recorded definition with its current one and lists the
 * incompatible changes - the ones that would change the meaning of values
 * already stored for the form's fields.
 * <p>
 * Both definitions are read as JSON trees (the form's JSON, or an array of
 * files for older records - see {@link FormDefinitionFactory#files}) and only the protected attributes
 * are looked up by key, so a definition recorded with an older configuration
 * format stays comparable.
 * <p>
 * Everything else - display names, descriptions, order, groups, properties,
 * mandatory, condition, placeholder, display format, layouts, active - may
 * change freely. Compatible changes: a new field, a new allowed value,
 * {@code multiple} (of a field or a block) from false to true, and data_type
 * STRING to LONG_STRING or EMAIL (stored values stay; an EMAIL value that is
 * not an e-mail address is shown as not valid and must be corrected). For FIXED fields (no stored values) only a switch to
 * or from DYNAMIC counts.
 */
public final class FormDefinitionComparator {

    private static final String FIXED = "FIXED";
    private static final String DYNAMIC = "DYNAMIC";

    private FormDefinitionComparator() {
    }

    public static List<FormDefinitionConflict> compare(
            @NotNull String formTitle, @NotNull JsonNode recorded, @NotNull JsonNode current) {
        Map<String, List<JsonNode>> recordedFields = instancesByLabel(recorded);
        Map<String, List<JsonNode>> currentFields = instancesByLabel(current);
        Map<String, Boolean> recordedBlocks = blockMultipleByLabel(recorded);
        Map<String, Boolean> currentBlocks = blockMultipleByLabel(current);

        List<FormDefinitionConflict> conflicts = new ArrayList<>();
        recordedFields.forEach((label, beforeInstances) -> {
            List<JsonNode> afterInstances = currentFields.get(label);
            if (afterInstances == null) {
                conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.FIELD_REMOVED, null, null));
                return;
            }
            // The instances of a label share these attributes (checked when the
            // configuration is loaded), so the first one stands for all of them.
            JsonNode before = beforeInstances.getFirst();
            JsonNode after = afterInstances.getFirst();
            String typeBefore = text(before, ProjectManagerConst.FORM_CONFIG_FIELD_TYPE, DYNAMIC);
            String typeAfter = text(after, ProjectManagerConst.FORM_CONFIG_FIELD_TYPE, DYNAMIC);
            if (!typeBefore.equals(typeAfter)) {
                conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.FIELD_TYPE_CHANGED, typeBefore, typeAfter));
                return;
            }
            if (FIXED.equals(typeAfter)) {
                return;
            }
            compareDynamicField(formTitle, label, beforeInstances, afterInstances, recordedBlocks, currentBlocks,
                    conflicts);
        });
        return conflicts;
    }

    private static void compareDynamicField(
            String formTitle, String label, List<JsonNode> beforeInstances, List<JsonNode> afterInstances,
            Map<String, Boolean> recordedBlocks, Map<String, Boolean> currentBlocks,
            List<FormDefinitionConflict> conflicts) {
        JsonNode before = beforeInstances.getFirst();
        JsonNode after = afterInstances.getFirst();
        String dataTypeBefore = text(before, ProjectManagerConst.FORM_CONFIG_DATA_TYPE, null);
        String dataTypeAfter = text(after, ProjectManagerConst.FORM_CONFIG_DATA_TYPE, null);
        if (!Objects.equals(dataTypeBefore, dataTypeAfter)
                && !("STRING".equals(dataTypeBefore)
                && ("LONG_STRING".equals(dataTypeAfter) || "EMAIL".equals(dataTypeAfter)))) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.DATA_TYPE_CHANGED, dataTypeBefore, dataTypeAfter));
        }

        // A value is only removed if no instance offers it anymore.
        Set<String> removedValues = allowedValueLabels(beforeInstances);
        removedValues.removeAll(allowedValueLabels(afterInstances));
        if (!removedValues.isEmpty()) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.ALLOWED_VALUES_REMOVED,
                    String.join(", ", removedValues), null));
        }

        String blockBefore = text(before, ProjectManagerConst.FORM_CONFIG_BLOCK, null);
        String blockAfter = text(after, ProjectManagerConst.FORM_CONFIG_BLOCK, null);
        if (!Objects.equals(blockBefore, blockAfter)) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.BLOCK_CHANGED, blockBefore, blockAfter));
        } else if (blockBefore != null
                && Boolean.TRUE.equals(recordedBlocks.get(blockBefore))
                && Boolean.FALSE.equals(currentBlocks.get(blockAfter))) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.BLOCK_MULTIPLE_DISABLED, blockBefore, null));
        }

        if (bool(before, ProjectManagerConst.FORM_CONFIG_MULTIPLE) && !bool(after, ProjectManagerConst.FORM_CONFIG_MULTIPLE)) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.MULTIPLE_DISABLED, "true", "false"));
        }

        if (bool(before, ProjectManagerConst.FORM_CONFIG_AS_FILE) != bool(after, ProjectManagerConst.FORM_CONFIG_AS_FILE)) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.AS_FILE_CHANGED,
                    String.valueOf(bool(before, ProjectManagerConst.FORM_CONFIG_AS_FILE)), String.valueOf(bool(after, ProjectManagerConst.FORM_CONFIG_AS_FILE))));
        }
    }

    // Every instance of each label, in configuration order.
    private static Map<String, List<JsonNode>> instancesByLabel(JsonNode definition) {
        return labelledEntries(definition, ProjectManagerConst.FORM_CONFIG_FIELDS)
                .collect(Collectors.groupingBy(FormDefinitionComparator::label, LinkedHashMap::new, Collectors.toList()));
    }

    private static Map<String, Boolean> blockMultipleByLabel(JsonNode definition) {
        return labelledEntries(definition, ProjectManagerConst.FORM_CONFIG_BLOCKS)
                .collect(Collectors.toMap(FormDefinitionComparator::label,
                        block -> bool(block, ProjectManagerConst.FORM_CONFIG_MULTIPLE),
                        (first, _) -> first, LinkedHashMap::new));
    }

    private static Set<String> allowedValueLabels(List<JsonNode> instances) {
        return instances.stream()
                .flatMap(field -> field.path(ProjectManagerConst.FORM_CONFIG_ALLOWED_VALUES).valueStream())
                .map(FormDefinitionComparator::label)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // The entries of a list property (e.g. "fields") of every file that have a label.
    private static Stream<JsonNode> labelledEntries(JsonNode definition, String property) {
        return FormDefinitionFactory.files(definition).stream()
                .flatMap(file -> file.path(property).valueStream())
                .filter(entry -> label(entry) != null);
    }

    private static String label(JsonNode node) {
        return text(node, ProjectManagerConst.FORM_CONFIG_LABEL, null);
    }

    private static String text(JsonNode node, String key, String defaultValue) {
        JsonNode value = node.get(key);
        return value == null || value.isNull() ? defaultValue : value.asText();
    }

    private static boolean bool(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null && value.asBoolean(false);
    }
}
