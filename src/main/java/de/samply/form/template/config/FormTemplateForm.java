package de.samply.form.template.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.form.core.model.DisplayMetadata;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A "forms" entry of a form template: a section of the generated form.
 * The list's order is the section order. {@code title} is an existing form's
 * title - then display_name/description, if set, override the form's own - or
 * a new, template-only section, filled by project fields whose form_title
 * names it.
 * <p>
 * An entry that only sets the position can be written as the plain title:
 * {@code "forms": ["query", {"title": "overview", "display_name": {...}}]}.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormTemplateForm extends DisplayMetadata {

    private String title;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public FormTemplateForm(String title) {
        this.title = title;
    }

}
