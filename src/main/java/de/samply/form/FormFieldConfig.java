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

    @JsonProperty("allowed_values")
    private FormFieldValue[] allowedValues;

    private boolean mandatory;

    // Inactive fields are hidden when a project has no data for them. They remain
    // available for projects with existing values, preserving previously saved data.
    private boolean active = true;

    // Special properties for different uses (e.g., something specific for the UI)
    private String[] properties;

    // Categories and subcategories of form fields
    private String[] groups;

    // A block is a collection of form fields that are always displayed together.
    private String block;

    // This field can also be provided as a file
    @JsonProperty("as_file")
    private Boolean asFile;

    // Condition for displaying the form field based on SpEL expression (e.g., "<label>.<value> == '12345'")
    // See https://docs.spring.io/spring-framework/reference/core/expressions.html
    // e.g. "condition": "['samples']['liquid_type']['value'] == 'other'"
    // The first element is the title, the second is the label, and the third one is an element from FormField.java (for frontend)
    // It should be written as in FormField.java
    private String condition;

    // This field is intended for project values to be displayed as form fields in a form.
    // e.g. "${project-code}": This will be replaced with the value of the project code.
    @JsonProperty("value")
    private String projectValue;

}
