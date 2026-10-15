package de.samply.frontend.dto.configuration;

import de.samply.form.core.FormConfig;
import de.samply.modules.OptionalModules;
import de.samply.project.ProjectType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
                .createProjectConfigurations(configuration, formConfigWithForms("project", "query"), allTypesAvailable()))
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
                .createProjectConfigurations(configuration, formConfigWithForms("project", "query"), allTypesAvailable()))
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
                .createProjectConfigurations(configuration, formConfigWithForms("project"), allTypesAvailable()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsRequestTypesWhoseModulesAreDisabled(@TempDir Path directory) throws Exception {
        Path configuration = directory.resolve("frontend-project-configs.json");
        Files.writeString(configuration, """
                {"formTitleOrder": [], "config": {
                  "DataSHIELD": {"project": {"outputs": [{"projectType": "DATASHIELD", "outputFormat": "OPAL", "templateId": "ccp"}]}},
                  "Export": {"project": {"outputs": [{"projectType": "EXPORT", "outputFormat": "EXCEL", "templateId": "ccp"}]}}}}
                """);
        OptionalModules optionalModules = mock(OptionalModules.class);
        when(optionalModules.isAvailable(any())).thenReturn(true);
        when(optionalModules.isAvailable(ProjectType.DATASHIELD)).thenReturn(false);
        when(optionalModules.describeUnavailable(ProjectType.DATASHIELD))
                .thenReturn("Request type DATASHIELD requires module DATASHIELD (ENABLE_DATASHIELD=false)");

        assertThatThrownBy(() -> new ProjectConfigurationsFactory()
                .createProjectConfigurations(configuration, formConfigWithForms(), optionalModules))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("1 request type(s) offered without their modules: configuration 'DataSHIELD': "
                        + "Request type DATASHIELD requires module DATASHIELD (ENABLE_DATASHIELD=false)");
    }

    private static OptionalModules allTypesAvailable() {
        OptionalModules optionalModules = mock(OptionalModules.class);
        when(optionalModules.isAvailable(any())).thenReturn(true);
        return optionalModules;
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
