package de.samply.form.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.form.FormFieldConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A "project_fields" entry of a form template: a {@link FormFieldConfig} with
 * the attributes only a form template uses. The forms' own field configs never
 * see them.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormTemplateFieldConfig extends FormFieldConfig {

    // The value shown, with project placeholders replaced, e.g. "${project-code}".
    @JsonProperty("value")
    private String projectValue;

    // The form (section) the field is printed in. Absent: the section of the
    // FIXED entry its label links to, else the header block before all sections.
    @JsonProperty("form_title")
    private String formTitle;

}
