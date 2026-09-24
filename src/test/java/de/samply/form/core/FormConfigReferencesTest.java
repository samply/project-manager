package de.samply.form.core;

import de.samply.utils.directory.ExistingDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormConfigReferencesTest {

    @Test
    void reportsEveryReferenceToSomethingNotConfigured(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("project.json"), """
                {"title": "project",
                 "groups": {"general": {"display_name": {"en": "General"}}},
                 "blocks": [{"label": "team"}],
                 "layouts": [{"rows": [{"fields": ["title", "subtitle"]}]}],
                 "fields": [
                   {"label": "title", "data_type": "STRING", "groups": ["general", "details"]},
                   {"label": "member", "data_type": "STRING", "block": "members"},
                   {"label": "reason", "data_type": "STRING",
                    "condition": "['ethics']['status']['value'] == 'approved' && ['ethics']['vote']['value'] == 'x'"}
                 ]}
                """);
        Files.writeString(directory.resolve("ethics.json"), """
                {"title": "ethics", "fields": [{"label": "status", "data_type": "STRING"}]}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("4 reference(s) to something not configured: "
                        + "group 'details' in project.json, form 'project', field 'title' is not defined in the form; "
                        + "block 'members' in project.json, form 'project', field 'member' is not defined in the form; "
                        + "condition in project.json, form 'project', field 'reason' refers to field ethics.vote, which does not exist; "
                        + "layout row in project.json, form 'project' names field 'subtitle', which the form does not have");
    }

    @Test
    void duplicatesAndBrokenReferencesAreReportedTogether(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("project.json"), """
                {"title": "project", "fields": [
                  {"label": "title", "data_type": "STRING"},
                  {"label": "title", "data_type": "STRING", "block": "missing"}
                ]}
                """);

        assertThatThrownBy(() -> new FormConfig(new ExistingDirectory(directory)))
                .hasMessageContaining("1 identifier(s) configured twice: field label 'title'")
                .hasMessageContaining(" | 1 reference(s) to something not configured: block 'missing'");
    }

    @Test
    void acceptsValidReferencesAcrossForms(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("project.json"), """
                {"title": "project",
                 "groups": {"general": {}}, "blocks": [{"label": "team"}],
                 "layouts": [{"rows": [{"fields": ["title", "member"]}]}],
                 "fields": [
                   {"label": "title", "data_type": "STRING", "groups": ["general"]},
                   {"label": "member", "data_type": "STRING", "block": "team",
                    "condition": "['ethics']['status']['value'] == 'approved'"}
                 ]}
                """);
        Files.writeString(directory.resolve("ethics.json"), """
                {"title": "ethics", "fields": [{"label": "status", "data_type": "STRING"}]}
                """);

        FormConfig config = new FormConfig(new ExistingDirectory(directory));

        assertThat(config.findMissingReferences("['ethics']['status']['value'] == 'x'")).isEmpty();
        assertThat(config.findMissingReferences("['ethics']['nope']['value'] == 'x'")).containsExactly("ethics.nope");
        assertThat(config.findMissingReferences(null)).isEmpty();
    }
}
