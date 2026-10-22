package de.samply.form.core;

import de.samply.form.core.model.FormFieldConfig;
import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The optional inclusive bounds "min" and "max" of INTEGER and DOUBLE fields. */
class FormConfigBoundsTest {

    @Test
    void boundsAreLoaded(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("patient.json"), """
                {"title": "patient",
                 "fields": [
                   {"label": "age", "data_type": "INTEGER", "min": 0, "max": 150},
                   {"label": "weight", "data_type": "DOUBLE", "min": 0.5},
                   {"label": "name", "data_type": "STRING"}
                 ]}
                """);

        FormConfig config = new FormConfig(new ExistingDirectory(directory));

        FormFieldConfig age = config.fetchFormFieldConfig("patient", "age");
        assertThat(age.getMin()).isEqualByComparingTo("0");
        assertThat(age.getMax()).isEqualByComparingTo("150");
        assertThat(config.fetchFormFieldConfig("patient", "weight").getMin()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(config.fetchFormFieldConfig("patient", "weight").getMax()).isNull();
        assertThat(config.fetchFormFieldConfig("patient", "name").getMin()).isNull();
    }

    @Test
    void boundsAreOnlySupportedForNumbers(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("patient.json"), """
                {"title": "patient",
                 "fields": [{"label": "name", "data_type": "STRING", "max": 10}]}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("field 'name': min and max are only supported for INTEGER and DOUBLE fields");
    }

    @Test
    void minMustNotBeGreaterThanMax(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("patient.json"), """
                {"title": "patient",
                 "fields": [{"label": "age", "data_type": "INTEGER", "min": 150, "max": 0}]}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("field 'age': min 150 is greater than max 0");
    }

    @Test
    void instancesMustHaveTheSameBoundsComparedByValue(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("patient.json"), """
                {"title": "patient",
                 "fields": [
                   {"label": "kind", "data_type": "STRING"},
                   {"label": "age", "data_type": "INTEGER", "min": 0, "max": 150,
                    "condition": "['patient']['kind']['value'] == 'adult'"},
                   {"label": "age", "data_type": "INTEGER", "min": 0.0, "max": 150.0},
                   {"label": "score", "data_type": "INTEGER", "max": 10,
                    "condition": "['patient']['kind']['value'] == 'adult'"},
                   {"label": "score", "data_type": "INTEGER", "max": 20}
                 ]}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("field label 'score' in patient.json, form 'patient': its instances differ in max")
                .hasMessageNotContaining("field label 'age'");
    }
}
