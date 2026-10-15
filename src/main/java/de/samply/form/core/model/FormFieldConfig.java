package de.samply.form.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.app.ProjectManagerConst;
import de.samply.display.DisplayFormatKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
public class FormFieldConfig extends ContextualDisplayMetadata {

    private String label;

    // DYNAMIC is the backward-compatible default and represents a normal,
    // persistable form field. FIXED identifies a metadata-only reference to a
    // frontend-implemented field. For FIXED fields, display metadata, order,
    // KEEP_FIXED_FIELD_ORDER, active state and condition are interpreted as
    // metadata for the native field; no value is persisted as a dynamic form
    // field.
    @JsonProperty(ProjectManagerConst.FORM_CONFIG_FIELD_TYPE)
    @Builder.Default
    private FormFieldType fieldType = FormFieldType.DYNAMIC;

    @JsonProperty(ProjectManagerConst.FORM_CONFIG_DATA_TYPE)
    private DataType dataType;

    /** Optional presentation override; does not affect input or stored values. */
    @JsonProperty(ProjectManagerConst.FORM_CONFIG_DISPLAY_FORMAT)
    private DisplayFormatKey displayFormat;

    public void validateDisplayFormat() {
        if (displayFormat == null) return;
        boolean dateOnly = displayFormat == DisplayFormatKey.DATE_FORMAT
                || displayFormat == DisplayFormatKey.LONG_DATE_FORMAT;
        if (dataType == DataType.TIMESTAMP || dataType == DataType.LOCAL_DATE_TIME
                || (dataType == DataType.DATE && dateOnly)) return;
        throw new IllegalArgumentException("Field '" + label + "': " + ProjectManagerConst.FORM_CONFIG_DISPLAY_FORMAT
                + " " + displayFormat + " is incompatible with " + ProjectManagerConst.FORM_CONFIG_DATA_TYPE + " " + dataType);
    }

    // Optional input hint for editable STRING and LONG_STRING fields.
    private String placeholder;

    @JsonProperty(ProjectManagerConst.FORM_CONFIG_ALLOWED_VALUES)
    private FormFieldAllowedValue[] allowedValues;

    private boolean mandatory;

    // Whether this field can hold several values of its own data type,
    // independently of any block-level "multiple" (FormFieldBlock.multiple).
    // Ignored for BOOLEAN fields.
    private boolean multiple = false;

    // For DYNAMIC fields, inactive definitions are hidden when a project has no
    // data for them and remain available when values already exist. For FIXED
    // fields, false is sent to the frontend to suppress its native field.
    @Builder.Default
    private boolean active = true;

    // Special properties for different uses (e.g., something specific for the UI)
    private String[] properties;

    // Categories and subcategories of form fields
    private String[] groups;

    // A block is a collection of form fields that are always displayed together.
    private String block;

    // This field can also be provided as a file
    @JsonProperty(ProjectManagerConst.FORM_CONFIG_AS_FILE)
    private Boolean asFile;

    // Condition for displaying the form field based on SpEL expression (e.g., "<label>.<value> == '12345'")
    // See https://docs.spring.io/spring-framework/reference/core/expressions.html
    // e.g. "condition": "['samples']['liquid_type']['value'] == 'other'"
    // The first element is the title, the second is the label, and the third one is an element from FormField.java (for frontend)
    // It should be written as in FormField.java
    // A label can be configured several times in a form ("instances"): the
    // instances are tried in order and the first whose condition holds is
    // shown, like if / else if. Every instance but the last needs a
    // condition; the last one without a condition is the default. All
    // instances describe one stored value, so data_type, field_type,
    // multiple, block and as_file must be the same in all of them (see
    // FormConfig.validateInstances).
    private String condition;

}
