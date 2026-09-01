package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.form.DataType;
import de.samply.form.FormFieldType;
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
        FormFieldValue[] allowedValues,
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
        String value
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
                .map(FormFieldValue::displayName)
                .findFirst()
                .orElse(value);
    }

}
