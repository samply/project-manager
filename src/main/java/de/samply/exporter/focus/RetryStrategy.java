package de.samply.exporter.focus;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RetryStrategy {

    @SuppressWarnings("SpellCheckingInspection")
    @JsonProperty("backoff_millisecs")
    private Integer backoffInMilliseconds;
    @JsonProperty("max_tries")
    private Integer maxTries;

}
