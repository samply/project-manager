package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.form.DataType;

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
        if (allowedValues != null) {
            for (FormFieldValue v : allowedValues) {
                if (v.label() != null && v.label().equals(value)) {
                    return v.displayName();
                }
            }
        }
        return value;
    }

}
