package de.samply.frontend;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import de.samply.utils.LanguageUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LanguageMessageMapDeserializer
        extends JsonDeserializer<Map<String, String>> {

    @Override
    public Map<String, String> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        Map<String, String> result = new HashMap<>();
        JsonNode node = p.getCodec().readTree(p);

        node.properties().forEach(entry ->
                result.put(
                        LanguageUtils.normalize(entry.getKey()),
                        entry.getValue().textValue()
                )
        );

        return result;
    }
}

