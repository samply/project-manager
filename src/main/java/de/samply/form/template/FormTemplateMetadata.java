package de.samply.form.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.utils.FileExtension;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateMetadata {

    private String template;
    @JsonProperty("form-titles")
    private String[] formTitles;
    @JsonProperty("filename-templates")
    private Map<FileExtension, String> extensionFilenameTemplateMap;
    @JsonProperty("variables")
    @JsonDeserialize(using = VariableLanguageValueMapDeserializer.class)
    private Map<String, Map<String, String>> variableLanguageValueMap;


}
