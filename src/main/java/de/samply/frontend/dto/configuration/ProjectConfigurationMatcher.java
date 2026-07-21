package de.samply.frontend.dto.configuration;

import de.samply.annotations.IgnoreProjectConfigurationMatch;
import de.samply.app.ProjectManagerConst;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.Project;
import de.samply.frontend.dto.ProjectAndForms;
import de.samply.frontend.dto.ProjectOutput;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProjectConfigurationMatcher {

    private static final String CUSTOM_KEY = ProjectManagerConst.CUSTOM_PROJECT_CONFIGURATION;

    private ProjectConfigurationMatcher() {
    }

    public static List<String> fetchMatchingProjectConfigurations(
            ProjectAndForms runtime,
            Map<String, ProjectAndForms> configurations,
            SelectionType selectionType) {

        if (runtime.project() != null && Boolean.TRUE.equals(runtime.project().getIsCustomConfigSelected())) {
            return List.of(CUSTOM_KEY);
        }

        List<ConfigurationMatch> matches = configurations.entrySet().stream()
                .filter(entry -> !CUSTOM_KEY.equals(entry.getKey()))
                .map(entry -> new ConfigurationMatch(
                        entry.getKey(),
                        calculateMatchScore(runtime, entry.getValue())))
                .filter(match -> match.score() >= 0)
                .toList();

        if (matches.isEmpty()) {
            return List.of(CUSTOM_KEY);
        }

        if (selectionType == SelectionType.MULTIPLE) {
            return matches.stream().map(ConfigurationMatch::name).toList();
        }

        return matches.stream()
                .max(Comparator.comparingInt(ConfigurationMatch::score))
                .map(match -> List.of(match.name()))
                .orElseGet(() -> List.of(CUSTOM_KEY));
    }

    private static int calculateMatchScore(ProjectAndForms runtime, ProjectAndForms template) {
        if (runtime == null || template == null) {
            return -1;
        }

        int projectScore = matchProject(runtime.project(), template.project());
        int formScore = matchForms(runtime.forms(), template.forms());
        int formFieldScore = matchFormFields(runtime.formFields(), template.formFields());

        if (projectScore < 0 || formScore < 0 || formFieldScore < 0) {
            return -1;
        }

        return projectScore + formScore + formFieldScore;
    }

    private static int matchProject(Project runtime, Project template) {
        if (template == null) {
            return 0;
        }
        if (runtime == null) {
            return -1;
        }

        int score = matchConfiguredFields(runtime, template, "outputs");
        if (score < 0) {
            return -1;
        }

        int outputScore = matchOutputs(runtime.getOutputs(), template.getOutputs());
        return outputScore < 0 ? -1 : score + outputScore;
    }

    private static int matchOutputs(ProjectOutput[] runtimeOutputs, ProjectOutput[] templateOutputs) {
        if (templateOutputs == null || templateOutputs.length == 0) {
            return 0;
        }
        if (runtimeOutputs == null || runtimeOutputs.length == 0) {
            return -1;
        }

        int score = 0;
        for (ProjectOutput templateOutput : templateOutputs) {
            int outputScore = Arrays.stream(runtimeOutputs)
                    .mapToInt(runtimeOutput -> matchConfiguredFields(runtimeOutput, templateOutput))
                    .filter(candidateScore -> candidateScore >= 0)
                    .max()
                    .orElse(-1);
            if (outputScore < 0) {
                return -1;
            }
            score += outputScore;
        }
        return score;
    }

    private static int matchForms(Form[] runtimeForms, Form[] templateForms) {
        if (templateForms == null || templateForms.length == 0) {
            return 0;
        }
        if (runtimeForms == null || runtimeForms.length == 0) {
            return -1;
        }

        int score = 0;
        for (Form templateForm : templateForms) {
            int formScore = Arrays.stream(runtimeForms)
                    .mapToInt(runtimeForm -> matchConfiguredFields(runtimeForm, templateForm))
                    .filter(candidateScore -> candidateScore >= 0)
                    .max()
                    .orElse(-1);
            if (formScore < 0) {
                return -1;
            }
            score += formScore;
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

        int score = 0;
        for (FormField templateField : templateFields) {
            int fieldScore = Arrays.stream(runtimeFields)
                    .mapToInt(runtimeField -> matchConfiguredFields(runtimeField, templateField))
                    .filter(candidateScore -> candidateScore >= 0)
                    .max()
                    .orElse(-1);
            if (fieldScore < 0) {
                return -1;
            }
            score += fieldScore;
        }
        return score;
    }

    /**
     * Treats the template as a subset: every non-null template field must equal the
     * runtime value, while values only present at runtime do not affect the match.
     */
    private static int matchConfiguredFields(Object runtime, Object template, String... excludedFields) {
        if (template == null) {
            return 0;
        }
        if (runtime == null || !runtime.getClass().equals(template.getClass())) {
            return -1;
        }

        List<String> exclusions = Arrays.asList(excludedFields);
        List<Field> fields = new ArrayList<>(Arrays.asList(template.getClass().getDeclaredFields()));
        int score = 0;

        try {
            for (Field field : fields) {
                if (field.isAnnotationPresent(IgnoreProjectConfigurationMatch.class)
                        || exclusions.contains(field.getName())) {
                    continue;
                }

                field.setAccessible(true);
                Object expected = field.get(template);
                if (expected == null) {
                    continue;
                }
                if (!Objects.deepEquals(expected, field.get(runtime))) {
                    return -1;
                }
                score++;
            }
            return score;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to compare project configuration", exception);
        }
    }

    private record ConfigurationMatch(String name, int score) {
    }
}
