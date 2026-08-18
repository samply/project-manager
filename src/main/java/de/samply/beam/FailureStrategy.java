package de.samply.beam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FailureStrategy {

    @JsonProperty("retry")
    private RetryStrategy retryStrategy;

}
