package de.samply.form.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.form.FormFieldConfig;
import de.samply.utils.FileExtension;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormTemplateMetadata {

    private String template;
    @JsonProperty("template_file")
    // Input template file
    // Without extension: Currently, only HTML files are supported
    private String templateFile;
    @JsonProperty("form_titles")
    private String[] formTitles;
    @JsonProperty("all_form_titles_required")
    private boolean allFormTitlesRequired = false;
    // Output filenames
    @JsonProperty("filename_templates")
    private Map<FileExtension, String> extensionFilenameTemplateMap;
    @JsonProperty("variables")
    @JsonDeserialize(using = VariableLanguageValueMapDeserializer.class)
    private Map<String, Map<String, String>> variableLanguageValueMap;
    @JsonProperty("display_name")
    private Map<String, String> languageDisplayNameMap;
    @JsonProperty("project_fields")
    @JsonDeserialize(using = FormFieldConfigDeserializer.class)
    private FormFieldConfig[] projectFields;


}
