package de.samply.form.template;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import de.samply.form.FormFieldConfig;

import java.io.IOException;
import java.util.UUID;

public class FormFieldConfigDeserializer extends JsonDeserializer<FormFieldConfig[]> {

    @Override
    public FormFieldConfig[] deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        JsonNode node = p.getCodec().readTree(p);

        if (!node.isArray()) {
            throw new IOException("Expected an array for projectFields");
        }

        FormFieldConfig[] fields = new FormFieldConfig[node.size()];

        for (int i = 0; i < node.size(); i++) {
            JsonNode fieldNode = node.get(i);

            // Use ObjectMapper to deserialize the individual object
            FormFieldConfig field = p.getCodec().treeToValue(fieldNode, FormFieldConfig.class);

            // Generate a UUID if label is missing or empty
            if (field.getLabel() == null || field.getLabel().isBlank()) {
                field.setLabel(UUID.randomUUID().toString());
            }

            fields[i] = field;
        }

        return fields;
    }

}
