package de.samply.frontend.dto;

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
        Boolean mandatory,
        String value
) {
}
