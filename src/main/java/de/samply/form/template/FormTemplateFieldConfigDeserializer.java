package de.samply.form.template;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.UUID;

public class FormTemplateFieldConfigDeserializer extends JsonDeserializer<FormTemplateFieldConfig[]> {

    @Override
    public FormTemplateFieldConfig[] deserialize(JsonParser p, DeserializationContext context) throws IOException {

        JsonNode node = p.getCodec().readTree(p);

        if (!node.isArray()) {
            throw new IOException("Expected an array for projectFields");
        }

        FormTemplateFieldConfig[] fields = new FormTemplateFieldConfig[node.size()];

        for (int i = 0; i < node.size(); i++) {
            JsonNode fieldNode = node.get(i);

            // Use ObjectMapper to deserialize the individual object
            FormTemplateFieldConfig field = p.getCodec().treeToValue(fieldNode, FormTemplateFieldConfig.class);

            // Generate a UUID if the label is missing or empty
            if (field.getLabel() == null || field.getLabel().isBlank()) {
                field.setLabel(UUID.randomUUID().toString());
            }

            fields[i] = field;
        }

        return fields;
    }

}
