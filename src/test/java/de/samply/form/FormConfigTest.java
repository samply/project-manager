package de.samply.form;

import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FormConfigTest {

    @Test
    void loadsLayoutsByFormTitle(@TempDir Path configDirectory) throws Exception {
        Files.writeString(configDirectory.resolve("patient.json"), """
                {
                  "title": "patient",
                  "fields": [],
                  "layouts": [
                    {
                      "rows": [
                        {"fields": ["patient-id", "birth-date"]},
                        {"fields": ["height", "weight"]}
                      ]
                    }
                  ]
                }
                """);

        FormConfig config = new FormConfig(new ExistingDirectory(configDirectory));

        assertThat(config.getFormTitleLayoutsMap())
                .containsExactly(Map.entry(
                        "patient",
                        List.of(new FormFieldLayout(List.of(
                                new FormFieldLayoutRow(List.of("patient-id", "birth-date")),
                                new FormFieldLayoutRow(List.of("height", "weight")))))));
    }
}
