package de.samply.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FormFieldConfig extends DisplayMetadata{

    private String label;

    @JsonProperty("data_type")
    private DataType dataType;

    private boolean mandatory;

    private String[] groups;

}
