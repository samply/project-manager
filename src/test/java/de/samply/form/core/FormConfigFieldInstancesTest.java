package de.samply.form.core;

import de.samply.form.core.model.FormFieldConfig;
import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A field label configured several times in one form: instances chosen in order by their conditions. */
class FormConfigFieldInstancesTest {

    @Test
    void instancesAreKeptInOrderAndTheFirstOneIsTheFieldsConfig(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("samples.json"), """
                {"title": "samples",
                 "fields": [
                   {"label": "liquid_type", "data_type": "STRING"},
                   {"label": "volume", "data_type": "STRING", "mandatory": true,
                    "condition": "['samples']['liquid_type']['value'] == 'blood'"},
                   {"label": "volume", "data_type": "STRING"},
                   {"label": "comment", "data_type": "STRING"}
                 ]}
                """);

        FormConfig config = new FormConfig(new ExistingDirectory(directory));

        assertThat(config.fetchFormFieldConfigs("samples", "volume"))
                .extracting(FormFieldConfig::isMandatory)
                .containsExactly(true, false);
        assertThat(config.fetchFormFieldConfig("samples", "volume").isMandatory()).isTrue();
        assertThat(config.fetchFormFieldConfigs("samples", "comment")).hasSize(1);
        assertThat(config.fetchFormFieldConfigs("samples", "missing")).isEmpty();
        // A field's position is that of its first instance; later fields follow without a gap.
        assertThat(config.getFormTitleLabelOrderMap().get("samples"))
                .containsEntry("liquid_type", 1)
                .containsEntry("volume", 2)
                .containsEntry("comment", 3);
    }

    @Test
    void everyInstanceButTheLastNeedsACondition(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("samples.json"), """
                {"title": "samples",
                 "fields": [
                   {"label": "liquid_type", "data_type": "STRING"},
                   {"label": "volume", "data_type": "STRING"},
                   {"label": "volume", "data_type": "STRING",
                    "condition": "['samples']['liquid_type']['value'] == 'blood'"}
                 ]}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("field label 'volume' in samples.json, form 'samples' "
                        + "(configured 2 times: every instance but the last needs a condition)");
    }

    @Test
    void theAttributesOfTheStoredValueMustBeTheSameInAllInstances(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("samples.json"), """
                {"title": "samples",
                 "blocks": [{"label": "sample"}],
                 "fields": [
                   {"label": "liquid_type", "data_type": "STRING"},
                   {"label": "volume", "data_type": "STRING", "block": "sample",
                    "condition": "['samples']['liquid_type']['value'] == 'blood'"},
                   {"label": "volume", "data_type": "INTEGER", "multiple": true}
                 ]}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1 field(s) with instances that do not fit together: "
                        + "field label 'volume' in samples.json, form 'samples': "
                        + "its instances differ in data_type, multiple, block, which must be the same in all of them");
    }

    @Test
    void aConditionOfAnInstanceMustNotReferToTheFieldItself(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("samples.json"), """
                {"title": "samples",
                 "fields": [
                   {"label": "volume", "data_type": "STRING",
                    "condition": "['samples']['volume']['value'] == null"},
                   {"label": "volume", "data_type": "STRING"}
                 ]}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("field label 'volume' in samples.json, form 'samples': "
                        + "the condition of instance 1 refers to the field itself");
    }

    @Test
    void aFixedFieldCanHaveInstancesWithinItsForm(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("project.json"), """
                {"title": "project",
                 "fields": [
                   {"label": "type", "data_type": "STRING"},
                   {"label": "PROJECT_TITLE", "field_type": "FIXED", "active": false,
                    "condition": "['project']['type']['value'] == 'internal'"},
                   {"label": "PROJECT_TITLE", "field_type": "FIXED"}
                 ]}
                """);

        FormConfig config = new FormConfig(new ExistingDirectory(directory));

        assertThat(config.fetchFormFieldConfigs("project", "PROJECT_TITLE"))
                .extracting(FormFieldConfig::isActive)
                .containsExactly(false, true);
    }

    @Test
    void referencesOfEveryInstanceAreChecked(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("samples.json"), """
                {"title": "samples",
                 "fields": [
                   {"label": "liquid_type", "data_type": "STRING"},
                   {"label": "volume", "data_type": "STRING",
                    "condition": "['samples']['liquid_type']['value'] == 'blood'"},
                   {"label": "volume", "data_type": "STRING", "groups": ["missing_group"],
                    "condition": "['samples']['missing_field']['value'] == 'x'"}
                 ]}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("group 'missing_group' in samples.json, form 'samples', field 'volume'")
                .hasMessageContaining("refers to field samples.missing_field, which does not exist");
    }
}
