package de.samply.form.core;

import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormConfigUniquenessTest {

    @Test
    void reportsEveryIdentifierConfiguredTwiceAtOnce(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("a-project.json"), """
                {"title": "project",
                 "blocks": [{"label": "team"}, {"label": "team"}],
                 "fields": [
                   {"label": "title", "data_type": "STRING"},
                   {"label": "title", "data_type": "STRING"},
                   {"label": "status", "data_type": "ENUM",
                    "allowed_values": [{"label": "ok"}, {"label": "ok"}]},
                   {"label": "PROJECT_TITLE", "field_type": "FIXED"}
                 ]}
                """);
        Files.writeString(directory.resolve("b-project-extra.json"), """
                {"title": "project", "fields": []}
                """);
        Files.writeString(directory.resolve("c-ethics.json"), """
                {"title": "ethics", "fields": [{"label": "PROJECT_TITLE", "field_type": "FIXED"}]}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("5 identifier(s) configured twice")
                .hasMessageContaining("field label 'title' in a-project.json, form 'project'")
                .hasMessageContaining("allowed value 'ok' in a-project.json, form 'project', field 'status'")
                .hasMessageContaining("block label 'team' in a-project.json, form 'project'")
                .hasMessageContaining("form title 'project' in a-project.json and b-project-extra.json (one file per form)")
                .hasMessageContaining("FIXED field label 'PROJECT_TITLE' in forms 'project' and 'ethics'");
    }

    @Test
    void blocksAndGroupsWithTheSameNameInTwoFormsAreSeparate(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("project.json"), """
                {"title": "project",
                 "groups": {"general": {"display_name": {"en": "Project in general"}}},
                 "blocks": [{"label": "team", "multiple": true}],
                 "fields": []}
                """);
        Files.writeString(directory.resolve("ethics.json"), """
                {"title": "ethics",
                 "groups": {"general": {"display_name": {"en": "Ethics in general"}}},
                 "blocks": [{"label": "team", "multiple": false}],
                 "fields": []}
                """);

        FormConfig config = new FormConfig(new ExistingDirectory(directory));

        assertThat(config.fetchGroup("project", "general").getDisplayName()).containsEntry("en", "Project in general");
        assertThat(config.fetchGroup("ethics", "general").getDisplayName()).containsEntry("en", "Ethics in general");
        assertThat(config.fetchBlock("project", "team").getMultiple()).isTrue();
        assertThat(config.fetchBlock("ethics", "team").getMultiple()).isFalse();
        assertThat(config.fetchBlock("funding", "team")).isNull();
    }

    @Test
    void aKeyTwiceInOneJsonObjectIsAnError(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("project.json"), """
                {"title": "project",
                 "groups": {"general": {"display_name": {"en": "A"}}, "general": {"display_name": {"en": "B"}}},
                 "fields": []}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("project.json")
                .hasMessageContaining("Duplicate field 'general'");
    }
}
