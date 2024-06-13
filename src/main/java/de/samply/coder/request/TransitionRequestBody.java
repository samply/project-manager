package de.samply.coder.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransitionRequestBody {

    @JsonProperty("transition")
    private String transition;

    @JsonProperty("orphan")
    private boolean orphan = false;

    public TransitionRequestBody() {
    }

    public TransitionRequestBody(String transition) {
        this.transition = transition;
    }

}
