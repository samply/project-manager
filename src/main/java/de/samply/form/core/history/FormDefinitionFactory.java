package de.samply.form.core.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.samply.form.core.FormConfig;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds each configured form's definition as it is stored in
 * {@code samply.form_history}: the form's whole JSON (its one configuration
 * file) in a canonical text form, and its checksum. Definitions recorded
 * before one file per form was enforced are JSON arrays of the form's files;
 * {@link #files} reads both.
 * <p>
 * Canonical: parsed and re-written with fixed indentation and "\n" line
 * breaks, keys in the order of the file. Reformatting a file, a byte order
 * mark, or the operating system's line separator therefore never changes the
 * checksum; only the content does. The file name is not part of it either,
 * so renaming a file changes nothing.
 */
@Component
public class FormDefinitionFactory {

    /** A form's canonical definition and its SHA-256 checksum (hex). */
    public record CanonicalFormDefinition(String formTitle, String definition, String checksum) {
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DefaultIndenter INDENTER = new DefaultIndenter("  ", "\n");
    private static final ObjectWriter CANONICAL_WRITER = OBJECT_MAPPER.writer(
            new DefaultPrettyPrinter().withObjectIndenter(INDENTER).withArrayIndenter(INDENTER));

    private final FormConfig formConfig;

    public FormDefinitionFactory(FormConfig formConfig) {
        this.formConfig = formConfig;
    }

    /** Every configured form's canonical definition, by form title. */
    public Map<String, CanonicalFormDefinition> fetchDefinitions() {
        Map<String, CanonicalFormDefinition> result = new LinkedHashMap<>();
        formConfig.getFormTitleJsonMap().forEach((formTitle, form) ->
                result.put(formTitle, createDefinition(formTitle, form)));
        return result;
    }

    public static CanonicalFormDefinition createDefinition(@NotNull String formTitle, @NotNull JsonNode form) {
        String definition = canonicalize(form);
        return new CanonicalFormDefinition(formTitle, definition, checksum(definition));
    }

    static String canonicalize(JsonNode form) {
        try {
            return CANONICAL_WRITER.writeValueAsString(form);
        } catch (JsonProcessingException e) {
            // A tree that was just parsed can always be written back.
            throw new IllegalStateException("Cannot write form definition", e);
        }
    }

    /**
     * The form objects of a recorded definition: the form itself, or - for a
     * definition recorded as an array of files - each file; none for the
     * "removed" marker (JSON null).
     */
    public static List<JsonNode> files(JsonNode definition) {
        if (definition == null || definition.isNull() || definition.isMissingNode()) {
            return List.of();
        }
        if (definition.isArray()) {
            List<JsonNode> result = new ArrayList<>();
            definition.forEach(result::add);
            return result;
        }
        return List.of(definition);
    }

    static String checksum(String definition) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(definition.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform is required to provide SHA-256.
            throw new IllegalStateException(e);
        }
    }
}
