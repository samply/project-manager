package de.samply.coder.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateRequestBody {

    @JsonProperty("name")
    private String name;

    @JsonProperty("rich_parameter_values")
    private CreateRequestParameter[] richParameterValues;

    @JsonProperty("template_version_id")
    private String templateVersionId;

}
