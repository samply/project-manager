package de.samply.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.directory.ExistingDirectory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Configuration
@Getter
public class FormConfig {

    private final Map<String, FormFieldBlock> blockLabelformFieldBlockMap = new HashMap<>();
    private final Map<String, ContextualDisplayMetadata> formTitleDisplaMetadataMap = new HashMap<>();
    private final Map<String, DisplayMetadata> groupsDisplayMetadataMap = new HashMap<>();
    private final Map<String, Map<String, FormFieldConfig>> formTitleLabelFieldMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> formTitleLabelOrderMap = new HashMap<>();
    private final Map<String, List<FormFieldLayout>> formTitleLayoutsMap = new HashMap<>();

    public FormConfig(@Value(ProjectManagerConst.FORM_FIELDS_DIRECTORY_SV) ExistingDirectory configDir
    ) {
        ObjectMapper objectMapper = new ObjectMapper();

        try (Stream<Path> files = Files.list(configDir.path())) {
            files
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted() // deterministic order
                    .forEach(path -> loadConfigFile(path, objectMapper));
        } catch (IOException e) {
            log.error("Failed to read form config directory {}", configDir, e);
        }
    }

    private void loadConfigFile(Path configFile, ObjectMapper objectMapper) {
        try {
            FormMetadataConfig formMetadataConfig =
                    objectMapper.readValue(configFile.toFile(), FormMetadataConfig.class);

            validateContextualInformation(formMetadataConfig, configFile);

            // Title metadata
            formTitleDisplaMetadataMap.put(
                    formMetadataConfig.getTitle(),
                    formMetadataConfig.fetchDisplayMetadata()
            );

            // Layouts
            List<FormFieldLayout> layouts = formTitleLayoutsMap.computeIfAbsent(
                    formMetadataConfig.getTitle(), _ -> new ArrayList<>());
            if (formMetadataConfig.getLayouts() != null) {
                layouts.addAll(Arrays.asList(formMetadataConfig.getLayouts()));
            }

            // Group metadata
            if (formMetadataConfig.getGroups() != null) {
                groupsDisplayMetadataMap.putAll(formMetadataConfig.getGroups());
            }

            // Fields + order
            Map<String, FormFieldConfig> fieldMap =
                    formTitleLabelFieldMap.computeIfAbsent(
                            formMetadataConfig.getTitle(),
                            _ -> new HashMap<>()
                    );

            Map<String, Integer> orderMap =
                    formTitleLabelOrderMap.computeIfAbsent(
                            formMetadataConfig.getTitle(),
                            _ -> new HashMap<>()
                    );

            FormFieldConfig[] fields = Optional.ofNullable(formMetadataConfig.getFields())
                    .orElseGet(() -> new FormFieldConfig[0]);
            AtomicInteger counter = new AtomicInteger(1);
            Arrays.stream(fields).forEach(field -> {
                fieldMap.put(field.getLabel(), field);
                orderMap.put(field.getLabel(), counter.getAndIncrement());
            });

            // Block metadata
            if (formMetadataConfig.getBlocks() != null) {
                Arrays.stream(formMetadataConfig.getBlocks()).forEach(fieldBlock ->
                        blockLabelformFieldBlockMap.put(fieldBlock.getLabel(), fieldBlock));
            }

            log.info(
                    "Loaded {} form fields from {}",
                    fields.length,
                    configFile.getFileName()
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read form config file " + configFile + ": " + e.getMessage(), e);
        }
    }

    private void validateContextualInformation(FormMetadataConfig form, Path configFile) {
        validateContextualInformation(form, configFile, "form '" + form.getTitle() + "'");

        if (form.getFields() != null) {
            Arrays.stream(form.getFields()).forEach(field -> validateContextualInformation(
                    field,
                    configFile,
                    "form '" + form.getTitle() + "', field '" + field.getLabel() + "'"));
        }

        if (form.getBlocks() != null) {
            Arrays.stream(form.getBlocks()).forEach(block -> validateContextualInformation(
                    block,
                    configFile,
                    "form '" + form.getTitle() + "', block '" + block.getLabel() + "'"));
        }
    }

    private void validateContextualInformation(
            ContextualDisplayMetadata metadata, Path configFile, String location) {
        validateDisplayInfo(metadata.getPreInfo(), configFile, location + ".pre_info");
        validateDisplayInfo(metadata.getPostInfo(), configFile, location + ".post_info");
    }

    private void validateDisplayInfo(DisplayInfo info, Path configFile, String location) {
        if (info != null && info.getProjectStates() != null && info.getProjectStates().isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid form configuration in " + configFile + " at " + location
                            + ": project_states must not be empty; omit project_states to allow all states");
        }
    }

    public FormFieldConfig fetchFormFieldConfig(String formTitle, String formLabel) {
        return formTitleLabelFieldMap.getOrDefault(formTitle, new HashMap<>()).get(formLabel);
    }

    public List<FormFieldConfig> fetchFieldsByTitleAndBlock(String title, String block) {
        return formTitleLabelFieldMap.getOrDefault(title, Map.of()).values().stream()
                .filter(config -> Objects.equals(config.getBlock(), block))
                .toList();
    }

}
