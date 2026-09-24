package de.samply.form.template.config;

import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormTemplateConfigUniquenessTest {

    @Test
    void reportsATemplateIdAndAProjectFieldLabelConfiguredTwice(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("a-request.json"), """
                {"template": "request", "project_fields": [
                  {"label": "code", "value": "${project-code}"},
                  {"label": "code", "value": "${project-title}"},
                  {"value": "no label"}, {"value": "no label either"}
                ]}
                """);
        Files.writeString(directory.resolve("b-request-copy.json"), """
                {"template": "request"}
                """);

        assertThatThrownBy(() -> new FormTemplateConfig(new ExistingDirectory(directory), "en"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2 identifier(s) configured twice")
                .hasMessageContaining("project field label 'code' in a-request.json, template 'request'")
                .hasMessageContaining("template 'request' in b-request-copy.json");
    }

    @Test
    void aKeyTwiceInOneJsonObjectIsAnError(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("request.json"), """
                {"template": "request", "display_name": {"en": "A", "en": "B"}}
                """);

        assertThatThrownBy(() -> new FormTemplateConfig(new ExistingDirectory(directory), "en"))
                .hasMessageContaining("request.json")
                .rootCause().hasMessageContaining("Duplicate field 'en'");
    }
}
