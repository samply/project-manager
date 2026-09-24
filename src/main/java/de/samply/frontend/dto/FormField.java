package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.display.DisplayFormatKey;
import de.samply.form.core.model.DataType;
import de.samply.form.core.model.FormFieldType;
import lombok.Builder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormField(
        String title,
        String titleDisplayName,
        String titleDescription,
        String titleShortDescription,
        String titlePreInfo,
        String titlePostInfo,
        String label,
        // DYNAMIC is a normal persistable form field. FIXED is a metadata-only
        // reference to a frontend field and must not enter dynamic persistence.
        // Older backend payloads may omit this; clients must default to DYNAMIC.
        FormFieldType fieldType,
        // Present only as false for an inactive FIXED field, telling the
        // frontend to suppress its native field. Missing means active/default;
        // DYNAMIC active state remains an internal backend concern.
        Boolean active,
        String labelDisplayName,
        String labelDescription,
        String labelShortDescription,
        String labelPreInfo,
        String labelPostInfo,
        String placeholder,
        FormFieldGroup[] groups,
        String[] properties,
        DataType type,
        DisplayFormatKey displayFormat,
        FormFieldAllowedValue[] allowedValues,
        Boolean mandatory,
        // Whether this field can hold several values of its own data type,
        // independently of any block-level "multiple" (multipleBlock below).
        // Ignored for BOOLEAN fields.
        Boolean multiple,
        Boolean asFile,
        String block,
        String blockDisplayName,
        String blockDescription,
        String blockShortDescription,
        String blockPreInfo,
        String blockPostInfo,
        Integer blockInstance,
        // Index of the value instance for a field whose config has
        // multiple = true. Scoped within blockInstance, not globally per
        // label - see ProjectFormField.fieldInstance for the full
        // explanation. Null when the field's config has multiple = false.
        Integer fieldInstance,
        Boolean multipleBlock,
        Integer minBlockInstances,
        Integer order,
        String value,
        @JsonIgnore String displayValue
) {

    // It can be used in the thymeleaf templates for the forms.
    @JsonIgnore
    @SuppressWarnings("unused")
    public String fetchDisplayValue() {
        return Optional
                .ofNullable(allowedValues)
                .stream()
                .flatMap(Arrays::stream)
                .filter(v -> Objects.equals(v.label(), value))
                .map(FormFieldAllowedValue::displayName)
                .findFirst()
                .orElse(displayValue != null ? displayValue : value);
    }

    // Description of the currently selected allowed value (an ENUM field's
    // own value-level description, distinct from labelDescription which
    // describes the field itself). Used in the thymeleaf templates for the
    // forms, to surface this alongside fetchDisplayValue(). Null when there
    // is no selected value, or no description configured for it. The full
    // description, else the short one (see fullOrShort).
    @JsonIgnore
    @SuppressWarnings("unused")
    public String fetchDisplayDescription() {
        return Optional
                .ofNullable(allowedValues)
                .stream()
                .flatMap(Arrays::stream)
                .filter(v -> Objects.equals(v.label(), value))
                .findFirst()
                // Optional.map (unlike Stream.findFirst()) tolerates a mapper
                // returning null, which description() often does.
                .map(FormField::fetchValueDescription)
                .orElse(null);
    }

    /**
     * An allowed value's description for the thymeleaf templates for the
     * forms: the full description, else the short one - or null if it only
     * repeats the value's display name (e.g. a unit "µl" described as "µl"),
     * which would print the same text twice.
     */
    public static String fetchValueDescription(FormFieldAllowedValue value) {
        String description = fullOrShort(value.description(), value.shortDescription());
        return description != null && value.displayName() != null
                && description.trim().equalsIgnoreCase(value.displayName().trim())
                ? null
                : description;
    }

    // The field's description for the thymeleaf templates for the forms: the
    // full description, else the short one (see fullOrShort).
    @JsonIgnore
    @SuppressWarnings("unused")
    public String fetchFullLabelDescription() {
        return fullOrShort(labelDescription, labelShortDescription);
    }

    /**
     * The full description, or the short one if the full one is missing or
     * blank; null if both are. Short descriptions are meant for places with
     * little room (e.g. the frontend Summary); a generated form document has
     * room for the full text.
     */
    public static String fullOrShort(String description, String shortDescription) {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return shortDescription != null && !shortDescription.isBlank() ? shortDescription : null;
    }

}
