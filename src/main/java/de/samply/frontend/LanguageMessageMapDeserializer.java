package de.samply.frontend;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import de.samply.utils.LanguageUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LanguageMessageMapDeserializer
        extends JsonDeserializer<Map<String, String>> {

    @Override
    public Map<String, String> deserialize(
            JsonParser p,
            DeserializationContext ctxt
    ) throws IOException {

        JsonNode node = p.getCodec().readTree(p);
        Map<String, String> result = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String normalizedKey = LanguageUtils.normalize(entry.getKey());
            result.put(normalizedKey, entry.getValue().asText());
        }

        return result;
    }
}
