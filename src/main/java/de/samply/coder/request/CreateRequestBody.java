package de.samply.coder.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Generate no-arg constructor
@AllArgsConstructor // Generate all-args constructor
@Builder(toBuilder = true)
public class CreateRequestBody {

    @JsonProperty("name")
    private String name;

    @JsonProperty("rich_parameter_values")
    private CreateRequestParameter[] richParameterValues;

    @JsonProperty("template_version_id")
    private String templateVersionId;

}
