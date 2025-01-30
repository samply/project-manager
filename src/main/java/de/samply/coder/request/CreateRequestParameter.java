package de.samply.coder.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor // Generate no-arg constructor
@AllArgsConstructor // Generate all-args constructor
public class CreateRequestParameter {

    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private String value;

}
