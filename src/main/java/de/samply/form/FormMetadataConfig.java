package de.samply.form;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormMetadataConfig extends DisplayMetadata{

    private String title;
    private Map<String, DisplayMetadata> groups;
    private FormFieldConfig[] fields;

}
