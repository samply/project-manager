package de.samply.batch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * One entry of an actions batch: a read action of the frontend and the parameters of its endpoint.
 *
 * @param action frontend action of a GET endpoint, e.g. FETCH_PROJECT
 * @param params parameters of the endpoint by name, as it would get them in its query string, e.g.
 *               {"project-code": "ABC-2026-0001", "bridgehead": "site-a"}. Optional.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ActionsBatchRequest(
        String action,
        Map<String, Object> params
) {
}
