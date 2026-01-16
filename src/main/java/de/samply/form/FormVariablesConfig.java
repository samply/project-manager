package de.samply.form;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.utils.LanguageUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
public class FormVariablesConfig {

    private final Map<String, Map<String, String>> labelLanguageValueMap;
    private final String defaultLanguage;

    public FormVariablesConfig(
            @Value(ProjectManagerConst.FORM_VARIABLES_PATH_SV) String formVariablesPath,
            @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage) throws IOException {

        this.defaultLanguage = LanguageUtils.normalize(defaultLanguage);

        // Read the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Map<String, String>> rawMap = objectMapper.readValue(
                new File(formVariablesPath),
                new TypeReference<>() {
                }
        );

        // Normalize all language keys in the map
        labelLanguageValueMap = rawMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // form variable key stays the same
                        e -> e.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        entry -> LanguageUtils.normalize(entry.getKey()), // normalize language key
                                        Map.Entry::getValue
                                ))
                ));
    }

    public Optional<String> getValue(String formVariable, String language) {
        Map<String, String> translations = labelLanguageValueMap.get(formVariable);
        if (translations == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(translations.getOrDefault(language, translations.get(defaultLanguage)));
    }

    public Map<String, String> getAllValues(String language) {
        return labelLanguageValueMap.keySet().stream()
                .map(key -> getValue(key, language).map(value -> Map.entry(key, value)))
                .flatMap(Optional::stream) // skip empty Optionals
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


}
