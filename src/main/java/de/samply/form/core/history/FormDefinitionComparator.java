package de.samply.form.core.history;

import com.fasterxml.jackson.databind.JsonNode;
import de.samply.form.core.history.FormDefinitionConflict.Kind;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 * STRING to LONG_STRING. For FIXED fields (no stored values) only a switch to
 * or from DYNAMIC counts.
 */
public final class FormDefinitionComparator {

    private static final String FIXED = "FIXED";
    private static final String DYNAMIC = "DYNAMIC";

    private FormDefinitionComparator() {
    }

    public static List<FormDefinitionConflict> compare(
            @NotNull String formTitle, @NotNull JsonNode recorded, @NotNull JsonNode current) {
        Map<String, JsonNode> recordedFields = fieldsByLabel(recorded);
        Map<String, JsonNode> currentFields = fieldsByLabel(current);
        Map<String, Boolean> recordedBlocks = blockMultipleByLabel(recorded);
        Map<String, Boolean> currentBlocks = blockMultipleByLabel(current);

        List<FormDefinitionConflict> conflicts = new ArrayList<>();
        recordedFields.forEach((label, before) -> {
            JsonNode after = currentFields.get(label);
            if (after == null) {
                conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.FIELD_REMOVED, null, null));
                return;
            }
            String typeBefore = text(before, "field_type", DYNAMIC);
            String typeAfter = text(after, "field_type", DYNAMIC);
            if (!typeBefore.equals(typeAfter)) {
                conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.FIELD_TYPE_CHANGED, typeBefore, typeAfter));
                return;
            }
            if (FIXED.equals(typeAfter)) {
                return;
            }
            compareDynamicField(formTitle, label, before, after, recordedBlocks, currentBlocks, conflicts);
        });
        return conflicts;
    }

    private static void compareDynamicField(
            String formTitle, String label, JsonNode before, JsonNode after,
            Map<String, Boolean> recordedBlocks, Map<String, Boolean> currentBlocks,
            List<FormDefinitionConflict> conflicts) {
        String dataTypeBefore = text(before, "data_type", null);
        String dataTypeAfter = text(after, "data_type", null);
        if (!Objects.equals(dataTypeBefore, dataTypeAfter)
                && !("STRING".equals(dataTypeBefore) && "LONG_STRING".equals(dataTypeAfter))) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.DATA_TYPE_CHANGED, dataTypeBefore, dataTypeAfter));
        }

        Set<String> removedValues = allowedValueLabels(before);
        removedValues.removeAll(allowedValueLabels(after));
        if (!removedValues.isEmpty()) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.ALLOWED_VALUES_REMOVED,
                    String.join(", ", removedValues), null));
        }

        String blockBefore = text(before, "block", null);
        String blockAfter = text(after, "block", null);
        if (!Objects.equals(blockBefore, blockAfter)) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.BLOCK_CHANGED, blockBefore, blockAfter));
        } else if (blockBefore != null
                && Boolean.TRUE.equals(recordedBlocks.get(blockBefore))
                && Boolean.FALSE.equals(currentBlocks.get(blockAfter))) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.BLOCK_MULTIPLE_DISABLED, blockBefore, null));
        }

        if (bool(before, "multiple") && !bool(after, "multiple")) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.MULTIPLE_DISABLED, "true", "false"));
        }

        if (bool(before, "as_file") != bool(after, "as_file")) {
            conflicts.add(new FormDefinitionConflict(formTitle, label, Kind.AS_FILE_CHANGED,
                    String.valueOf(bool(before, "as_file")), String.valueOf(bool(after, "as_file"))));
        }
    }

    private static Map<String, JsonNode> fieldsByLabel(JsonNode definition) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (JsonNode file : FormDefinitionFactory.files(definition)) {
            for (JsonNode field : file.path("fields")) {
                String label = text(field, "label", null);
                if (label != null) {
                    result.putIfAbsent(label, field);
                }
            }
        }
        return result;
    }

    private static Map<String, Boolean> blockMultipleByLabel(JsonNode definition) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (JsonNode file : FormDefinitionFactory.files(definition)) {
            for (JsonNode block : file.path("blocks")) {
                String label = text(block, "label", null);
                if (label != null) {
                    result.putIfAbsent(label, bool(block, "multiple"));
                }
            }
        }
        return result;
    }

    private static Set<String> allowedValueLabels(JsonNode field) {
        Set<String> result = new LinkedHashSet<>();
        for (JsonNode value : field.path("allowed_values")) {
            String label = text(value, "label", null);
            if (label != null) {
                result.add(label);
            }
        }
        return result;
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
