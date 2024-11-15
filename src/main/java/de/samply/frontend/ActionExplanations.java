package de.samply.frontend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionExplanations {

    @JsonProperty("explanations")
    private Map<String, List<ActionExplanation>> actionExplanationMap;

    public Optional<List<ActionExplanation>> getActionExplanation(String action) {
        return Optional.ofNullable(actionExplanationMap.get(action));
    }

}
