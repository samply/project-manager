package de.samply.frontend.dto.configuration;

import de.samply.app.ProjectManagerConst;
import de.samply.frontend.dto.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ProjectConfigurationMatcher {

    private static final String CUSTOM_KEY = ProjectManagerConst.CUSTOM_PROJECT_CONFIGURATION;

    public static Map<String, ProjectAndForms> fetchMatchProjectConfiguration(ProjectAndForms runtime, Map<String, ProjectAndForms> config) {

        if (runtime.project() != null && runtime.project().isCustomConfigSelected()) {
            return fetchCustomConfiguration(config);
        }

        Map<Map.Entry<String, ProjectAndForms>, Integer> matchScores = new HashMap<>();

        for (Map.Entry<String, ProjectAndForms> templateEntry : config.entrySet()) {

            if (templateEntry.getKey().equals(CUSTOM_KEY)) {
                continue;
            }

            int score = calculateMatchScore(runtime, templateEntry.getValue());

            if (score >= 0) {
                matchScores.put(templateEntry, score);
            }
        }

        return matchScores.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(e -> Map.of(e.getKey().getKey(), e.getKey().getValue()))
                .orElseGet(() -> fetchCustomConfiguration(config));
    }

    private static Map<String, ProjectAndForms> fetchCustomConfiguration(
            Map<String, ProjectAndForms> config) {

        ProjectAndForms custom = config.get(CUSTOM_KEY);

        if (custom != null) {
            return Map.of(CUSTOM_KEY, custom);
        }

        // fallback minimal custom
        Project customProject = new Project();
        customProject.setCustomConfigSelected(true);

        return Map.of(CUSTOM_KEY,
                new ProjectAndForms(customProject, new Form[0], new FormField[0]));
    }

    private static int calculateMatchScore(
            ProjectAndForms runtime,
            ProjectAndForms template) {

        int projectScore = matchProject(runtime.project(), template.project());
        if (projectScore < 0) return -1;

        int formScore = matchForms(runtime.forms(), template.forms());
        if (formScore < 0) return -1;

        int formFieldScore = matchFormFields(runtime.formFields(), template.formFields());
        if (formFieldScore < 0) return -1;

        return projectScore + formScore + formFieldScore;
    }

    private static int matchProject(Project runtime, Project template) {
        if (runtime == null || template == null) return -1;

        // Check the project-level field
        if (template.isCustomConfigSelected() && !runtime.isCustomConfigSelected()) return -1;

        ProjectOutput[] templateOutputs = template.getOutputs();
        ProjectOutput[] runtimeOutputs = runtime.getOutputs();

        if (templateOutputs == null || templateOutputs.length == 0) return 0;

        if (runtimeOutputs == null || runtimeOutputs.length != templateOutputs.length) return -1;

        // Compute score using streams
        return (template.isCustomConfigSelected() ? 1 : 0) +
                IntStream.range(0, templateOutputs.length)
                        .map(i -> matchOutput(runtimeOutputs[i], templateOutputs[i]))
                        .filter(s -> s >= 0) // only count successful matches
                        .sum();
    }

    private static int matchOutput(ProjectOutput runtime, ProjectOutput template) {
        if (template.projectType() != null && !Objects.equals(template.projectType(), runtime.projectType())) return -1;
        if (template.outputFormat() != null && !Objects.equals(template.outputFormat(), runtime.outputFormat()))
            return -1;
        if (template.templateId() != null && !Objects.equals(template.templateId(), runtime.templateId())) return -1;

        // Count matched fields
        return Stream.of(template.projectType(), template.outputFormat(), template.templateId())
                .filter(Objects::nonNull)
                .mapToInt(_ -> 1)
                .sum();
    }

    private static int matchForms(Form[] runtimeForms, Form[] templateForms) {

        if (templateForms == null || templateForms.length == 0) {
            return 0;
        }

        if (runtimeForms == null || runtimeForms.length == 0) {
            return -1;
        }

        Set<String> runtimeTitles = Arrays.stream(runtimeForms)
                .map(Form::title)
                .collect(Collectors.toSet());

        int score = 0;

        for (Form templateForm : templateForms) {
            if (!runtimeTitles.contains(templateForm.title())) {
                return -1;
            }
            score++;
        }

        return score;
    }

    private static int matchFormFields(FormField[] runtimeFields, FormField[] templateFields) {

        if (templateFields == null || templateFields.length == 0) {
            return 0;
        }

        if (runtimeFields == null || runtimeFields.length == 0) {
            return -1;
        }

        Map<String, FormField> runtimeLookup = Arrays.stream(runtimeFields)
                .collect(Collectors.toMap(
                        f -> buildKey(f.title(), f.label()),
                        f -> f,
                        (a, _) -> a
                ));

        int score = 0;

        for (FormField templateField : templateFields) {

            String key = buildKey(templateField.title(), templateField.label());
            FormField runtimeField = runtimeLookup.get(key);

            if (runtimeField == null) {
                return -1;
            }

            if (templateField.value() != null &&
                    !Objects.equals(templateField.value(), runtimeField.value())) {
                return -1;
            }

            score++;
        }

        return score;
    }

    private static String buildKey(String title, String label) {
        return (title == null ? "" : title) + "|" + (label == null ? "" : label);
    }
}
