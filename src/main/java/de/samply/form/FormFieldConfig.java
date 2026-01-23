package de.samply.form;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormFieldConfig extends DisplayMetadata {

    private String label;

    @JsonProperty("data_type")
    private DataType dataType;

    private boolean mandatory;

    private String[] groups;

    // This field is intended for project values to be displayed as form fields in a form.
    // e.g. "${project-code}": This will be replaced with the value of the project code.
    @JsonProperty("value")
    private String projectValue;

}
