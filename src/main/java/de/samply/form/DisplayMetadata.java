package de.samply.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.utils.LanguageUtils;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents metadata that provides localized display names, descriptions and short descriptions
 * for various entities, such as forms or groups.
 *
 * <p>This class ensures that all language codes are stored in lowercase,
 * even after deserialization.</p>
 *
 * @author Your Name
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class DisplayMetadata {

    @JsonProperty("display_name")
    private Map<String, String> displayName = new HashMap<>();

    @JsonProperty("description")
    private Map<String, String> description = new HashMap<>();

    @JsonProperty("short_description")
    private Map<String, String> shortDescription = new HashMap<>();

    /**
     * Ensures that language keys in displayName, description and shortDescription are stored in lowercase.
     * This is executed automatically after object creation or deserialization.
     */
    @PostConstruct
    private void normalizeLanguageKeys() {
        displayName = normalizeLanguageMap(displayName);
        description = normalizeLanguageMap(description);
        shortDescription = normalizeLanguageMap(shortDescription);
    }

    private static Map<String, String> normalizeLanguageMap(Map<String, String> values) {
        if (values == null) {
            return null;
        }
        return values.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> LanguageUtils.normalize(entry.getKey()),
                        Map.Entry::getValue,
                        (existing, _) -> existing,
                        HashMap::new
                ));
    }

    public DisplayMetadata fetchDisplayMetadata() {
        DisplayMetadata result = new DisplayMetadata();
        result.setDisplayName(displayName);
        result.setDescription(description);
        result.setShortDescription(shortDescription);
        return result;
    }

}
