package de.samply.form;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormMetadataConfig extends DisplayMetadata{

    private String title;
    private Map<String, DisplayMetadata> groups;
    private FormFieldConfig[] fields;
    private FormFieldBlock[] fieldBlocks;

}
