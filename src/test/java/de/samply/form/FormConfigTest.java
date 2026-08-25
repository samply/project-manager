package de.samply.form;

import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormConfigTest {

    @Test
    void loadsMetadataOnlyFormWithEmptyFields(@TempDir Path configDirectory) throws Exception {
        Files.writeString(configDirectory.resolve("services.json"), """
                {
                  "title": "services",
                  "display_name": {"en": "Requested Resources"},
                  "description": {"en": "Select the requested resource categories."},
                  "fields": []
                }
                """);

        FormConfig config = new FormConfig(new ExistingDirectory(configDirectory));

        assertThat(config.getFormTitleDisplaMetadataMap().get("services"))
                .satisfies(metadata -> {
                    assertThat(metadata.getDisplayName()).containsEntry("en", "Requested Resources");
                    assertThat(metadata.getDescription())
                            .containsEntry("en", "Select the requested resource categories.");
                });
        assertThat(config.getFormTitleLabelFieldMap().get("services")).isEmpty();
        assertThat(config.getFormTitleLabelOrderMap().get("services")).isEmpty();
    }

    @Test
    void treatsOmittedFieldsAsEmptyForMetadataOnlyForm(@TempDir Path configDirectory) throws Exception {
        Files.writeString(configDirectory.resolve("summary.json"), """
                {
                  "title": "summary",
                  "display_name": {"en": "Summary"}
                }
                """);

        FormConfig config = new FormConfig(new ExistingDirectory(configDirectory));

        assertThat(config.getFormTitleDisplaMetadataMap()).containsKey("summary");
        assertThat(config.getFormTitleLabelFieldMap().get("summary")).isEmpty();
    }

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

    @Test
    void rejectsExplicitlyEmptyProjectStateRestrictionsDuringStartup(@TempDir Path configDirectory)
            throws Exception {
        Path configFile = configDirectory.resolve("patient.json");
        Files.writeString(configFile, """
                {
                  "title": "patient",
                  "fields": [{
                    "label": "status",
                    "data_type": "STRING",
                    "pre_info": {
                      "content": {"en": "Invalid restriction"},
                      "project_states": []
                    }
                  }]
                }
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(configDirectory)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(configFile.toString())
                .hasMessageContaining("form 'patient', field 'status'.pre_info")
                .hasMessageContaining("project_states must not be empty")
                .hasMessageContaining("omit project_states to allow all states");
    }
}
