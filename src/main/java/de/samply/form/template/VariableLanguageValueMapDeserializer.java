package de.samply.form.template;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import de.samply.utils.LanguageUtils;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class VariableLanguageValueMapDeserializer
        extends StdDeserializer<Map<String, Map<String, String>>> {

    public VariableLanguageValueMapDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public Map<String, Map<String, String>> deserialize(
            JsonParser p,
            DeserializationContext context) throws IOException {

        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        Map<String, Map<String, String>> raw =
                mapper.convertValue(
                        node,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                        }
                );

        if (raw == null) {
            return Map.of();
        }

        return raw.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        entry -> LanguageUtils.normalize(entry.getKey()),
                                        Map.Entry::getValue
                                ))
                ));
    }

}
