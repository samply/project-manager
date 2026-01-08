package de.samply.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.utils.LanguageUtils;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents metadata that provides localized display names and descriptions
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
public class DisplayMetadata {

    @JsonProperty("display_name")
    private Map<String, String> displayName = new HashMap<>();

    @JsonProperty("description")
    private Map<String, String> description = new HashMap<>();

    /**
     * Ensures that language keys in displayName and description are stored in lowercase.
     * This is executed automatically after object creation or deserialization.
     */
    @PostConstruct
    private void normalizeLanguageKeys() {
        if (displayName != null) {
            displayName = displayName.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> LanguageUtils.normalize(entry.getKey()),
                            Map.Entry::getValue,
                            (existing, _) -> existing,
                            HashMap::new
                    ));
        }

        if (description != null) {
            description = description.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> LanguageUtils.normalize(entry.getKey()),
                            Map.Entry::getValue,
                            (existing, _) -> existing,
                            HashMap::new
                    ));
        }
    }

    public DisplayMetadata fetchDisplayMetadata() {
        DisplayMetadata result = new DisplayMetadata();
        result.setDisplayName(displayName);
        result.setDescription(description);
        return result;
    }

}
