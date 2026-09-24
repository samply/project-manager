package de.samply.form.core.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.form.core.FormConfig;
import de.samply.form.core.history.FormDefinitionFactory.CanonicalFormDefinition;
import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FormDefinitionFactoryTest {

    private static final String ETHICS = """
            {
              "title": "ethics",
              "display_name": {"en": "Ethics"},
              "fields": [
                {"label": "status", "data_type": "ENUM",
                 "allowed_values": [{"label": "approved", "display_name": {"en": "Approved"}}]}
              ]
            }
            """;

    @Test
    void formattingByteOrderMarkAndLineSeparatorsDoNotChangeTheChecksum(
            @TempDir Path compact, @TempDir Path reformatted) throws Exception {
        Files.writeString(compact.resolve("ethics.json"), ETHICS.replaceAll("\\s*\\n\\s*", ""));
        Files.write(reformatted.resolve("ethics.json"),
                ("﻿" + ETHICS.replace("\n", "\r\n").replace("  ", "    ")).getBytes(StandardCharsets.UTF_8));

        assertThat(definition(compact, "ethics").checksum()).isEqualTo(definition(reformatted, "ethics").checksum());
    }

    @Test
    void theDefinitionIsTheWholeJsonWithItsKeyOrderAndUnixLineBreaks(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("ethics.json"), ETHICS);

        String definition = definition(directory, "ethics").definition();

        assertThat(definition).startsWith("{").doesNotContain("\r")
                .contains("\"display_name\"", "\"Approved\"");
        assertThat(definition.indexOf("\"title\"")).isLessThan(definition.indexOf("\"display_name\""));
        assertThat(definition.indexOf("\"display_name\"")).isLessThan(definition.indexOf("\"fields\""));
    }

    @Test
    void aContentChangeChangesTheChecksumButARenamedFileDoesNot(
            @TempDir Path original, @TempDir Path renamed, @TempDir Path changed) throws Exception {
        Files.writeString(original.resolve("ethics.json"), ETHICS);
        Files.writeString(renamed.resolve("ethics-deprecated-at-2026-09-24.json"), ETHICS);
        Files.writeString(changed.resolve("ethics.json"), ETHICS.replace("\"ENUM\"", "\"STRING\""));

        String checksum = definition(original, "ethics").checksum();

        assertThat(definition(renamed, "ethics").checksum()).isEqualTo(checksum);
        assertThat(definition(changed, "ethics").checksum()).isNotEqualTo(checksum);
    }

    @Test
    void eachFormIsItsOwnDefinition(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("ethics.json"), ETHICS);
        Files.writeString(directory.resolve("funding.json"), """
                {"title": "funding", "fields": []}
                """);

        Map<String, CanonicalFormDefinition> definitions =
                new FormDefinitionFactory(new FormConfig(new ExistingDirectory(directory))).fetchDefinitions();

        assertThat(definitions).containsOnlyKeys("ethics", "funding");
        assertThat(definitions.get("funding").definition()).startsWith("{").doesNotContain("\"status\"");
    }

    @Test
    void readsTheFormOfARecordedDefinitionOfEitherShape() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        assertThat(FormDefinitionFactory.files(mapper.readTree("{\"title\": \"ethics\"}"))).hasSize(1);
        assertThat(FormDefinitionFactory.files(mapper.readTree("[{\"title\": \"a\"}, {\"title\": \"a\"}]"))).hasSize(2);
        assertThat(FormDefinitionFactory.files(mapper.readTree("null"))).isEmpty();
    }

    private static CanonicalFormDefinition definition(Path directory, String formTitle) {
        return new FormDefinitionFactory(new FormConfig(new ExistingDirectory(directory)))
                .fetchDefinitions().get(formTitle);
    }
}
