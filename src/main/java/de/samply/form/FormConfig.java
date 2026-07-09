package de.samply.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
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
    private final Map<String, DisplayMetadata> formTitleDisplaMetadataMap = new HashMap<>();
    private final Map<String, DisplayMetadata> groupsDisplayMetadataMap = new HashMap<>();
    private final Map<String, Map<String, FormFieldConfig>> formTitleLabelFieldMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> formTitleLabelOrderMap = new HashMap<>();

    public FormConfig(@Value(ProjectManagerConst.FORM_FIELDS_DIRECTORY_SV) Path configDir
    ) {
        ObjectMapper objectMapper = new ObjectMapper();

        if (!Files.isDirectory(configDir)) {
            log.warn("Form config directory does not exist or is not a directory: {}", configDir);
            return;
        }

        try (Stream<Path> files = Files.list(configDir)) {
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

            // Title metadata
            formTitleDisplaMetadataMap.put(
                    formMetadataConfig.getTitle(),
                    formMetadataConfig.fetchDisplayMetadata()
            );

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

            AtomicInteger counter = new AtomicInteger(1);
            Arrays.stream(formMetadataConfig.getFields()).forEach(field -> {
                fieldMap.put(field.getLabel(), field);
                orderMap.put(field.getLabel(), counter.getAndIncrement());
            });

            // Block metadata
            if (formMetadataConfig.getFieldBlocks() != null) {
                Arrays.stream(formMetadataConfig.getFieldBlocks()).forEach(fieldBlock ->
                        blockLabelformFieldBlockMap.put(fieldBlock.getLabel(), fieldBlock));
            }

            log.info(
                    "Loaded {} form fields from {}",
                    formMetadataConfig.getFields().length,
                    configFile.getFileName()
            );

        } catch (IOException e) {
            log.error("Failed to read form config file {}", configFile, e);
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
