package de.samply.feasibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.frontend.dto.FeasibilityItem;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FeasibilityMapper {

    private final String mapping;
    private final ObjectMapper objectMapper;

    private Scope rootScope;
    private JsonQuery compiledQuery;

    public FeasibilityMapper(@Value(ProjectManagerConst.FEASIBILITY_MAPPING_SV) String mapping) {
        this.mapping = mapping;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    void init() throws JsonQueryException {
        rootScope = Scope.newEmptyScope();
        BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, rootScope);

        if (mapping != null && !mapping.isBlank()) {
            compiledQuery = JsonQuery.compile(mapping.strip(), Versions.JQ_1_6);
            log.info("Feasibility JQ mapping compiled successfully");
        } else {
            log.info("No feasibility.mapping configured — will return all totals as-is");
        }
    }

    public List<FeasibilityItem> map(JsonNode rawResult) {
        if (compiledQuery == null) {
            return mapAll(rawResult);
        }
        try {
            Scope scope = Scope.newChildScope(rootScope);
            List<JsonNode> outputs = new ArrayList<>();
            compiledQuery.apply(scope, rawResult, outputs::add);

            if (outputs.isEmpty() || !outputs.get(0).isArray()) {
                log.warn("Feasibility JQ mapping produced no array output — falling back to all totals");
                return mapAll(rawResult);
            }
            return parseItems(outputs.get(0));
        } catch (JsonQueryException e) {
            log.error("Feasibility JQ mapping failed: {}", e.getMessage(), e);
            return mapAll(rawResult);
        }
    }

    private List<FeasibilityItem> parseItems(JsonNode arrayNode) {
        List<FeasibilityItem> items = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            String label = node.path("label").asText(null);
            if (label == null) continue;
            long value = node.path("value").asLong(0);
            JsonNode breakdownNode = node.path("breakdown");
            List<FeasibilityItem> breakdown = (breakdownNode.isArray() && !breakdownNode.isEmpty())
                    ? parseItems(breakdownNode)
                    : null;
            items.add(new FeasibilityItem(label, value, breakdown));
        }
        return items;
    }

    /**
     * Fallback: no feasibility.mapping configured in application.yaml.
     * Returns every key in "totals" as a flat list.
     */
    private List<FeasibilityItem> mapAll(JsonNode rawResult) {
        List<FeasibilityItem> items = new ArrayList<>();
        rawResult.path("totals").fields().forEachRemaining(entry ->
                items.add(new FeasibilityItem(entry.getKey(), entry.getValue().asLong(), null)));
        return items;
    }
}
