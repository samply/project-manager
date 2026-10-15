package de.samply.batch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * What one entry of an actions batch answers: the response of its endpoint, or the error the endpoint would have
 * answered with.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActionsBatchResult(
        // Body of a successful answer; absent when the endpoint answers without a body
        JsonNode response,
        // HTTP status the endpoint would have answered with; only for a failed entry
        Integer errorCode,
        String errorMessage,
        String errorStacktrace,
        long durationMs
) {

    public static ActionsBatchResult success(JsonNode response, long durationMs) {
        return new ActionsBatchResult(response, null, null, null, durationMs);
    }

    public static ActionsBatchResult error(int errorCode, String errorMessage, String errorStacktrace, long durationMs) {
        return new ActionsBatchResult(null, errorCode, errorMessage, errorStacktrace, durationMs);
    }

}
