package de.samply.frontend.dto.configuration;

import de.samply.form.core.FormConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                .createProjectConfigurations(configuration, formConfigWithForms("project", "query")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate entries in formTitleOrder: project");
    }

    @Test
    void rejectsAConfigurationNamingAFormThatDoesNotExist(@TempDir Path directory) throws Exception {
        Path configuration = directory.resolve("frontend-project-configs.json");
        Files.writeString(configuration, """
                {
                  "formTitleOrder": ["query", "project"],
                  "config": {
                    "Export": {"forms": [{"title": "project"}, {"title": "ethics-v2"}]},
                    "Custom": {"forms": [{"title": "query"}]}
                  }
                }
                """);

        assertThatThrownBy(() -> new ProjectConfigurationsFactory()
                .createProjectConfigurations(configuration, formConfigWithForms("project", "query")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("1 reference(s) to something not configured: "
                        + "configuration 'Export' names form 'ethics-v2', which does not exist");
    }

    @Test
    void acceptsConfigurationsNamingExistingForms(@TempDir Path directory) throws Exception {
        Path configuration = directory.resolve("frontend-project-configs.json");
        Files.writeString(configuration, """
                {"formTitleOrder": [], "config": {"Export": {"forms": [{"title": "project"}]}}}
                """);

        assertThatCode(() -> new ProjectConfigurationsFactory()
                .createProjectConfigurations(configuration, formConfigWithForms("project")))
                .doesNotThrowAnyException();
    }

    private static FormConfig formConfigWithForms(String... titles) {
        FormConfig formConfig = mock(FormConfig.class);
        Map<String, Map<String, de.samply.form.core.model.FormFieldConfig>> forms = new java.util.HashMap<>();
        for (String title : titles) {
            forms.put(title, Map.of());
        }
        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(forms);
        return formConfig;
    }
}
