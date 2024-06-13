package de.samply.coder.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateRequestParameter {

    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private String value;

    public CreateRequestParameter() {
    }

    public CreateRequestParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

}
