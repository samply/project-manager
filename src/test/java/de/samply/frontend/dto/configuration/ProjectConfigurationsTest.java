package de.samply.frontend.dto.configuration;

import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.Project;
import de.samply.frontend.dto.ProjectAndForms;
import de.samply.frontend.dto.ProjectOutput;
import de.samply.project.ProjectType;
import de.samply.query.OutputFormat;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectConfigurationsTest {

    @Test
    void multipleSelectionReturnsEverySubsetMatch() {
        ProjectConfigurations configurations = configurations(SelectionType.MULTIPLE);
        Project runtimeProject = project(
                output(ProjectType.EXPORT, OutputFormat.CSV, "export"),
                output(ProjectType.DATASHIELD, OutputFormat.OPAL, "datashield"));

        List<String> result = configurations.fetchCurrentProjectConfiguration(
                new ProjectAndForms(runtimeProject, new Form[0], new FormField[0]));

        assertThat(result).containsExactly("export", "datashield");
    }

    @Test
    void singleSelectionKeepsOnlyTheMostSpecificMatch() {
        ProjectConfigurations configurations = configurations(SelectionType.SINGLE);
        Project combined = project(
                output(ProjectType.EXPORT, OutputFormat.CSV, "export"),
                output(ProjectType.DATASHIELD, OutputFormat.OPAL, "datashield"));
        configurations.getConfig().put("combined", configuration(combined));

        List<String> result = configurations.fetchCurrentProjectConfiguration(
                new ProjectAndForms(combined, new Form[0], new FormField[0]));

        assertThat(result).containsExactly("combined");
    }

    @Test
    void mergeCombinesConfigurationsAndLetsLaterValuesWin() {
        ProjectConfigurations configurations = configurations(SelectionType.MULTIPLE);
        FormField first = FormField.builder().title("request").label("format").value("csv").build();
        FormField second = first.toBuilder().value("opal").build();
        configurations.getConfig().put("export", new ProjectAndForms(
                project(output(ProjectType.EXPORT, OutputFormat.CSV, "export")),
                new Form[]{new Form("export", null, null)},
                new FormField[]{first}));
        configurations.getConfig().put("datashield", new ProjectAndForms(
                project(output(ProjectType.DATASHIELD, OutputFormat.OPAL, "datashield")),
                new Form[]{new Form("datashield", null, null)},
                new FormField[]{second}));

        ProjectAndForms merged = configurations.merge(List.of("export", "datashield"));

        assertThat(merged.project().getOutputs()).hasSize(2);
        assertThat(merged.forms()).extracting(Form::title).containsExactly("export", "datashield");
        assertThat(merged.formFields()).extracting(FormField::value).containsExactly("opal");
    }

    @Test
    void parseSelectionKeepsFirstNameForSingleSelection() {
        ProjectConfigurations configurations = configurations(SelectionType.SINGLE);

        assertThat(configurations.parseSelection("export,datashield"))
                .containsExactly("export");
    }

    private ProjectConfigurations configurations(SelectionType selectionType) {
        ProjectConfigurations configurations = new ProjectConfigurations();
        configurations.setSelectionType(selectionType);
        Map<String, ProjectAndForms> config = new LinkedHashMap<>();
        config.put("export", configuration(project(output(ProjectType.EXPORT, OutputFormat.CSV, "export"))));
        config.put("datashield", configuration(project(output(ProjectType.DATASHIELD, OutputFormat.OPAL, "datashield"))));
        configurations.setConfig(config);
        return configurations;
    }

    private ProjectAndForms configuration(Project project) {
        return new ProjectAndForms(project, new Form[0], new FormField[0]);
    }

    private Project project(ProjectOutput... outputs) {
        Project project = new Project();
        project.setOutputs(outputs);
        return project;
    }

    private ProjectOutput output(ProjectType type, OutputFormat format, String templateId) {
        return new ProjectOutput(type, format, templateId);
    }
}
