package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.form.DataType;
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
        String labelDisplayName,
        String labelDescription,
        String labelShortDescription,
        String labelPreInfo,
        String labelPostInfo,
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
