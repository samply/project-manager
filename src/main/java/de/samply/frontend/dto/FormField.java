package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.form.DataType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormField(
        String title,
        String titleDisplayName,
        String titleDescription,
        String label,
        String labelDisplayName,
        String labelDescription,
        FormFieldGroup[] groups,
        DataType type,
        FormFieldValue[] allowedValues,
        Boolean mandatory,
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
