package de.samply.form.core.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.samply.form.core.history.FormDefinitionConflict.Kind;
import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Explains a form's incompatible changes to the configuration maintainer and
 * generates the fix to paste into the form's JSON file:
 * <ul>
 *   <li>a changed field: the recorded field, marked {@code "active": false}
 *   (projects keep their values under it), and the current field under a new
 *   label ({@code status} → {@code status-v2}, {@code status-v2} →
 *   {@code status-v3}), used from now on;</li>
 *   <li>a removed field: the recorded field, marked {@code "active": false},
 *   to add back;</li>
 *   <li>a removed or renamed form: its recorded definition, marked
 *   {@code "active": false}, to restore, and the title for its replacement
 *   ({@code ethics} → {@code ethics-v2}).</li>
 * </ul>
 */
public final class FormDefinitionFix {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DefaultIndenter INDENTER = new DefaultIndenter("  ", "\n");
    private static final ObjectWriter WRITER = OBJECT_MAPPER.writer(
            new DefaultPrettyPrinter().withObjectIndenter(INDENTER).withArrayIndenter(INDENTER));
    private static final Pattern VERSIONED_LABEL = Pattern.compile("^(.*)-v(\\d+)$");
    private static final String INDENT = "    ";

    private FormDefinitionFix() {
    }

    /**
     * @param recorded the form's latest recorded definition
     * @param current  its current definition, or null if the form is no longer configured
     */
    public static String explain(@NotNull String formTitle, int recordedVersion, @NotNull JsonNode recorded,
                                 JsonNode current, @NotNull List<FormDefinitionConflict> conflicts) {
        StringBuilder result = new StringBuilder()
                .append("Incompatible change in form \"").append(formTitle)
                .append("\" (recorded version ").append(recordedVersion).append("):\n");
        conflicts.forEach(conflict -> result.append("  - ").append(conflict.describe()).append('\n'));

        if (current == null) {
            return result.append("""
                    Projects selected this form or stored values in it. Keep the form, marked inactive - \
                    projects that use it keep it, no other project is offered it (its recorded definition):

                    """).append(indent(write(inactiveForm(recorded)))).append("""


                    If it was renamed, give the new form the title "%s" and add that title in \
                    frontend-project-configs.json (the configurations' "forms" and "formTitleOrder").
                    """.formatted(nextLabel(formTitle, Set.of()))).toString();
        }

        Map<String, JsonNode> recordedFields = fieldsByLabel(recorded);
        Set<String> usedLabels = new LinkedHashSet<>(fieldsByLabel(current).keySet());
        Map<String, List<FormDefinitionConflict>> byField = conflicts.stream()
                .filter(conflict -> conflict.label() != null)
                .collect(Collectors.groupingBy(FormDefinitionConflict::label, LinkedHashMap::new, Collectors.toList()));

        result.append("""
                Stored values keep their meaning only under the recorded definition. In the form's JSON \
                file ("fields"), keep the recorded field inactive - projects keep their values under it - \
                and, for a changed field, add its new definition under a new label:
                """);
        byField.forEach((label, fieldConflicts) -> {
            JsonNode recordedField = recordedFields.get(label);
            if (recordedField == null) {
                return;
            }
            result.append("\nField \"").append(label).append("\":\n\n")
                    .append(indent(write(inactive(recordedField))));
            boolean removed = fieldConflicts.stream().anyMatch(conflict -> conflict.kind() == Kind.FIELD_REMOVED);
            JsonNode currentField = fieldsByLabel(current).get(label);
            if (!removed && currentField != null) {
                String newLabel = nextLabel(label, usedLabels);
                usedLabels.add(newLabel);
                result.append(",\n").append(indent(write(relabelled(currentField, newLabel))));
            }
            result.append('\n');
        });

        List<String> relabelled = byField.entrySet().stream()
                .filter(entry -> entry.getValue().stream().noneMatch(conflict -> conflict.kind() == Kind.FIELD_REMOVED))
                .map(entry -> "\"" + entry.getKey() + "\"")
                .toList();
        if (!relabelled.isEmpty()) {
            result.append("\nThen update what still refers to ").append(String.join(", ", relabelled))
                    .append(": conditions (e.g. ['").append(formTitle).append("']['<label>']['value']), ")
                    .append("layouts, and form-template project fields.\n");
        }
        return result.toString();
    }

    /** The next free versioned label: {@code status} → {@code status-v2}, {@code status-v2} → {@code status-v3}. */
    static String nextLabel(String label, Set<String> usedLabels) {
        Matcher matcher = VERSIONED_LABEL.matcher(label);
        String base = matcher.matches() ? matcher.group(1) : label;
        int version = matcher.matches() ? Integer.parseInt(matcher.group(2)) + 1 : 2;
        while (usedLabels.contains(base + "-v" + version)) {
            version++;
        }
        return base + "-v" + version;
    }

    private static JsonNode inactive(JsonNode field) {
        ObjectNode copy = field.deepCopy();
        copy.put("active", false);
        return copy;
    }

    // The form-level "active": false on the form (on each file of an older,
    // multi-file record).
    private static JsonNode inactiveForm(JsonNode definition) {
        JsonNode copy = definition.deepCopy();
        FormDefinitionFactory.files(copy).forEach(file -> {
            if (file instanceof ObjectNode form) {
                form.put("active", false);
            }
        });
        return copy;
    }

    private static JsonNode relabelled(JsonNode field, String label) {
        ObjectNode copy = field.deepCopy();
        copy.put("label", label);
        return copy;
    }

    private static Map<String, JsonNode> fieldsByLabel(JsonNode definition) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (JsonNode file : FormDefinitionFactory.files(definition)) {
            for (JsonNode field : file.path("fields")) {
                JsonNode label = field.get("label");
                if (label != null && !label.isNull()) {
                    result.putIfAbsent(label.asText(), field);
                }
            }
        }
        return result;
    }

    private static String write(JsonNode node) {
        try {
            return WRITER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot write form definition", e);
        }
    }

    private static String indent(String text) {
        return text.lines().map(line -> INDENT + line).collect(Collectors.joining("\n"));
    }
}
