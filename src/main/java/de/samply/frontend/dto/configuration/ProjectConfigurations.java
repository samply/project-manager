package de.samply.frontend.dto.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.samply.app.ProjectManagerConst;
import de.samply.frontend.dto.*;
import de.samply.project.ProjectType;
import lombok.Data;

import java.util.*;

@Data
public class ProjectConfigurations {

    // If not specifically set, the user can choose only one project configuration.
    private SelectionType selectionType = SelectionType.SINGLE;
    private Map<String, ProjectAndForms> config = new HashMap<>();

    private boolean isMultipleSelection(){
        return selectionType == SelectionType.MULTIPLE;
    }

    @JsonIgnore
    public List<String> fetchCurrentProjectConfiguration(ProjectAndForms projectAndForms) {
        return ProjectConfigurationMatcher.fetchMatchingProjectConfigurations(
                projectAndForms, config, selectionType);
    }

    @JsonIgnore
    public List<ProjectAndForms> fetchCurrentProjectConfigurations(ProjectAndForms projectAndForms) {
        return fetchCurrentProjectConfiguration(projectAndForms).stream()
                .map(config::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @JsonIgnore
    public List<String> parseSelection(String selectedConfigurations) {
        if (selectedConfigurations == null) {
            throw new IllegalArgumentException("At least one project configuration must be selected");
        }

        List<String> names = Arrays.stream(selectedConfigurations.split(ProjectManagerConst.PROJECT_CONFIGURATION_SEPARATOR))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();

        if (names.isEmpty()) {
            throw new IllegalArgumentException("At least one project configuration must be selected");
        }
        if (names.contains(ProjectManagerConst.NOT_SELECTED_PROJECT_CONFIGURATION) && names.size() > 1) {
            throw new IllegalArgumentException("The NOT_SELECTED project configuration cannot be combined with another configuration");
        }
        if (!isMultipleSelection() && names.size() > 1) {
            names = List.of(names.getFirst());
        }
        if (names.contains(ProjectManagerConst.CUSTOM_PROJECT_CONFIGURATION) && names.size() > 1) {
            throw new IllegalArgumentException("The custom project configuration cannot be combined with another configuration");
        }
        names.stream()
                .filter(name -> !ProjectManagerConst.CUSTOM_PROJECT_CONFIGURATION.equals(name))
                .filter(name -> !config.containsKey(name))
                .findFirst()
                .ifPresent(name -> {
                    throw new IllegalArgumentException("Project configuration " + name + " not found");
                });

        return names;
    }

    @JsonIgnore
    public ProjectAndForms merge(Collection<String> configurationNames) {
        Project mergedProject = new Project();
        Map<ProjectType, ProjectOutput> outputs = new LinkedHashMap<>();
        Map<String, Form> forms = new LinkedHashMap<>();
        Map<FormFieldKey, FormField> formFields = new LinkedHashMap<>();

        configurationNames.stream()
                .map(config::get)
                .filter(Objects::nonNull)
                .forEach(configuration -> {
                    mergeProject(mergedProject, configuration.project(), outputs);
                    if (configuration.forms() != null) {
                        Arrays.stream(configuration.forms())
                                .forEach(form -> forms.put(form.title(), form));
                    }
                    if (configuration.formFields() != null) {
                        Arrays.stream(configuration.formFields())
                                .forEach(field -> formFields.put(FormFieldKey.from(field), field));
                    }
                });

        mergedProject.setOutputs(outputs.values().toArray(ProjectOutput[]::new));
        return new ProjectAndForms(
                mergedProject,
                forms.values().toArray(Form[]::new),
                formFields.values().toArray(FormField[]::new));
    }

    private void mergeProject(Project target, Project source, Map<ProjectType, ProjectOutput> outputs) {
        if (source == null) {
            return;
        }

        if (source.getExpiresAt() != null) target.setExpiresAt(source.getExpiresAt());
        if (source.getQuery() != null) target.setQuery(source.getQuery());
        if (source.getHumanReadable() != null) target.setHumanReadable(source.getHumanReadable());
        if (source.getQueryFormat() != null) target.setQueryFormat(source.getQueryFormat());
        if (source.getQueryContext() != null) target.setQueryContext(source.getQueryContext());

        if (source.getOutputs() != null) {
            Arrays.stream(source.getOutputs())
                    .forEach(output -> outputs.put(output.projectType(), output));
        }
    }

    private record FormFieldKey(String title, String label, Integer blockInstance) {
        private static FormFieldKey from(FormField field) {
            return new FormFieldKey(field.title(), field.label(), field.blockInstance());
        }
    }

}

