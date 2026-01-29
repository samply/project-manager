package de.samply.form.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.form.FormFieldConfig;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class FormTemplateConfig {

    @Getter
    private final Map<String, FormTemplateMetadata> templateMetadataMap;
    private final Map<String, Map<String, Integer>> templateLabelOrderMap;
    private final String defaultLanguage;

    public FormTemplateConfig(
            @Value(ProjectManagerConst.FORM_TEMPLATE_METADATA_DIRECTORY_SV) Path templatesDir,
            @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage
    ) {
        this.defaultLanguage = defaultLanguage;
        this.templateMetadataMap = loadTemplates(new ObjectMapper(), templatesDir);
        this.templateLabelOrderMap = loadTemplateLabelOrderMap(templateMetadataMap);
    }

    public Optional<FormTemplateMetadata> getTemplate(String key) {
        return Optional.ofNullable(templateMetadataMap.get(key));
    }

    public boolean isProjectFormFieldTitle(String formFieldTitle) {
        return formFieldTitle.startsWith(ProjectManagerConst.FORM_PROJECT_FIELDS_TITLE_PREFIX);
    }

    public String fetchProjectFormFieldTitle(String formTemplate) {
        return ProjectManagerConst.FORM_PROJECT_FIELDS_TITLE_PREFIX + formTemplate;
    }

    public int fetchProjectFormFieldOrder(String projectFormFieldTitle, String formFieldLabel) {
        String prefix = ProjectManagerConst.FORM_PROJECT_FIELDS_TITLE_PREFIX;

        if (!isProjectFormFieldTitle(projectFormFieldTitle)) {
            throw new IllegalArgumentException(
                    "Invalid formFieldTitle: must start with " + prefix + ", got: " + projectFormFieldTitle);
        }

        String template = projectFormFieldTitle.substring(prefix.length());

        Map<String, Integer> labelOrder = templateLabelOrderMap.get(template);
        if (labelOrder == null) {
            throw new IllegalArgumentException("No template found for name: " + template);
        }

        Integer priority = labelOrder.get(formFieldLabel);
        if (priority == null) {
            throw new IllegalArgumentException(
                    "Label '" + formFieldLabel + "' not found in template '" + template + "'");
        }

        return priority;
    }

    private Map<String, FormTemplateMetadata> loadTemplates(
            ObjectMapper objectMapper,
            Path templatesDir
    ) {
        if (!Files.isDirectory(templatesDir)) {
            throw new IllegalStateException(
                    "Form templates directory does not exist or is not a directory: " + templatesDir
            );
        }
        try (Stream<Path> paths = Files.list(templatesDir)) {
            return loadTemplates(objectMapper, paths);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load form templates from directory: " + templatesDir, e
            );
        }
    }

    private Map<String, Map<String, Integer>> loadTemplateLabelOrderMap(Map<String, FormTemplateMetadata> templateMetadataMap) {
        return templateMetadataMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // template name
                        entry -> {
                            FormFieldConfig[] fields = entry.getValue().getProjectFields();
                            if (fields == null || fields.length == 0) {
                                return Map.of(); // empty map if no fields
                            }
                            Map<String, Integer> labelOrder = new HashMap<>();
                            for (int i = 0; i < fields.length; i++) {
                                // last element = -1, first element = -length
                                labelOrder.put(fields[i].getLabel(), i - fields.length);
                            }
                            return labelOrder;
                        }
                ));
    }

    private Map<String, FormTemplateMetadata> loadTemplates(ObjectMapper objectMapper, Stream<Path> paths) {
        Map<String, FormTemplateMetadata> result = new HashMap<>();
        paths
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(path -> loadTemplates(objectMapper, result, path));
        return result;
    }

    private void loadTemplates(ObjectMapper objectMapper, Map<String, FormTemplateMetadata> result, Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            FormTemplateMetadata metadata =
                    objectMapper.readValue(is, FormTemplateMetadata.class);
            result.put(metadata.getTemplate(), metadata);

        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read form template: " + path, e
            );
        }
    }

    public Optional<String> fetchFormVariable(String template, String formVariable, String language) {
        return Optional.ofNullable(templateMetadataMap.get(template))
                .map(FormTemplateMetadata::getVariableLanguageValueMap)
                .map(m -> m.get(formVariable))
                .map(translations ->
                        translations.getOrDefault(language, translations.get(defaultLanguage))
                );
    }

    public Map<String, String> fetchAllFormVariables(@NotNull String template, @NotNull String language) {
        return Optional.ofNullable(templateMetadataMap.get(template))
                .stream()
                .flatMap(metadata ->
                        metadata.getVariableLanguageValueMap().keySet().stream()
                                .flatMap(key ->
                                        fetchFormVariable(template, key, language)
                                                .stream()
                                                .map(value -> Map.entry(key, value))
                                )
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }


}