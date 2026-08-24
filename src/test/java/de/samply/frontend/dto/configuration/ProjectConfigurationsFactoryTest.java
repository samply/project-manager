package de.samply.frontend.dto.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectConfigurationsFactoryTest {

    @Test
    void rejectsDuplicateFormTitleOrderEntriesDuringStartup(@TempDir Path directory) throws Exception {
        Path configuration = directory.resolve("frontend-project-configs.json");
        Files.writeString(configuration, """
                {
                  "formTitleOrder": ["project", "query", "project"],
                  "config": {}
                }
                """);

        assertThatThrownBy(() -> new ProjectConfigurationsFactory()
                .createProjectConfigurations(configuration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate entries in formTitleOrder: project");
    }
}
