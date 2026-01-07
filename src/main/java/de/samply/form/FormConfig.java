package de.samply.form;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration class responsible for loading form field configurations from external JSON files.
 * <p>
 * This class retrieves environment variables that start with the prefix {@code FORM_CONFIG_PATH}
 * and loads their corresponding JSON files to populate a list of {@link FormFieldConfig} objects.
 * </p>
 *
 * <h3>Behavior:</h3>
 * <ul>
 *     <li>Checks all environment variables that start with {@code FORM_CONFIG_PATH}.</li>
 *     <li>Sorts the variables alphabetically by their key names for consistent ordering.</li>
 *     <li>Reads JSON configuration files from the specified paths and converts them into a list of form fields.</li>
 *     <li>Logs warnings and errors for missing or invalid files but continues processing remaining valid files.</li>
 * </ul>
 *
 * <h3>Example Environment Variables:</h3>
 * <pre>
 * FORM_CONFIG_PATH=/path/to/main_config.json
 * FORM_CONFIG_PATH_2=/path/to/second_config.json
 * FORM_CONFIG_PATH_EXTRA=/path/to/extra_config.json
 * </pre>
 *
 * <h3>Example JSON Configuration:</h3>
 * <pre>
 * [
 *   {
 *     "label": "Gender",
 *     "data_type": "STRING",
 *     "mandatory": true,
 *     "display_text": {
 *       "en": "Gender",
 *       "de": "Geschlecht"
 *     }
 *   }
 * ]
 * </pre>
 *
 * <h3>Usage:</h3>
 * <p>Once the application starts, this configuration class will populate the {@code formFields} list with the loaded form definitions.</p>
 */
@Slf4j
@Configuration
@Getter
public class FormConfig {

    private final Map<String, DisplayMetadata> formTitleDisplaMetadataMap = new HashMap<>();
    private final Map<String, DisplayMetadata> groupsDisplayMetadataMap = new HashMap<>();
    private final Map<String, Map<String, FormFieldConfig>> formTitleLabelFieldMap = new HashMap<>();

    /**
     * Constructor that initializes the form field configurations based on environment variables.
     *
     * @param env The Spring Environment, used to retrieve environment variables.
     */
    public FormConfig(Environment env) {
        ObjectMapper objectMapper = new ObjectMapper();

        // Fetch all system properties and environment variables
        Map<String, Object> envVars = ((AbstractEnvironment) env).getSystemProperties();
        envVars.putAll(((AbstractEnvironment) env).getSystemEnvironment());

        // Extract all environment variables that start with "FORM_CONFIG_PATH"
        List<String> configPaths = envVars.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ProjectManagerConst.FORM_CONFIG_PATH_PREFIX))
                .sorted(Map.Entry.comparingByKey()) // Ensures consistent order of processing
                .map(entry -> entry.getValue().toString()) // Convert values to Strings
                .toList();

        if (configPaths.isEmpty()) {
            log.warn("No environment variables with prefix 'FORM_CONFIG_PATH' found. Using empty form configuration.");
            return;
        }

        // Load form configurations from each valid file path
        for (String configPath : configPaths) {
            if (!StringUtils.hasText(configPath)) {
                log.warn("Ignoring empty configuration path.");
                continue;
            }

            File configFile = new File(configPath);
            if (!configFile.exists() || !configFile.isFile()) {
                log.error("Configuration file not found at: {}", configPath);
                continue;
            }

            try {
                FormMetadataConfig formMetadataConfig = objectMapper.readValue(configFile, new TypeReference<>() {
                });
                this.formTitleDisplaMetadataMap.put(formMetadataConfig.getTitle(), formMetadataConfig.fetchDisplayMetadata());
                if (formMetadataConfig.getGroups() != null) {
                    this.groupsDisplayMetadataMap.putAll(formMetadataConfig.getGroups());
                }
                Arrays.stream(formMetadataConfig.getFields()).forEach(formFieldConfig -> {
                    // Add form field config to map
                    Map<String, FormFieldConfig> formFieldLabelConfigMap = formTitleLabelFieldMap.get(formMetadataConfig.getTitle());
                    if (formFieldLabelConfigMap == null) {
                        formFieldLabelConfigMap = new HashMap<>();
                        formTitleLabelFieldMap.put(formMetadataConfig.getTitle(), formFieldLabelConfigMap);
                    }
                    formFieldLabelConfigMap.put(formFieldConfig.getLabel(), formFieldConfig);
                });
                log.info("Successfully loaded {} form fields from {}", formMetadataConfig.getFields().length, configPath);
            } catch (IOException e) {
                log.error("Failed to read form config file at {}", configPath, e);
            }
        }
    }

}

