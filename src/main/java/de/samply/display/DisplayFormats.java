package de.samply.display;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.utils.LanguageUtils;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DisplayFormats {

    private Map<DisplayFormatKey, Map<String, String>> formats = Map.of();
    private Map<String, String> locales = Map.of();
    private Map<String, String> legacyNumberFormats = Map.of();

    public Map<String, String> getLocales() {
        return locales;
    }

    public void setLocales(Map<String, String> locales) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (locales != null) {
            locales.forEach((language, locale) -> {
                String key = LanguageUtils.normalize(language);
                if (normalized.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate locale language: " + language);
                }
                normalized.put(key, locale);
            });
        }
        this.locales = Collections.unmodifiableMap(normalized);
    }

    /** Compatibility reader for the previous numberFormats: {language: {locale}} shape. */
    @JsonProperty("numberFormats")
    public void setLegacyNumberFormats(Map<String, NumberDisplayFormat> numberFormats) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (numberFormats != null) {
            numberFormats.forEach((language, format) -> normalized.put(
                    LanguageUtils.normalize(language), format == null ? null : format.locale()));
        }
        this.legacyNumberFormats = Collections.unmodifiableMap(normalized);
    }

    Map<String, String> getLegacyNumberFormats() {
        return legacyNumberFormats;
    }

    public static DisplayFormats of(Map<DisplayFormatKey, Map<String, String>> formats) {
        DisplayFormats result = new DisplayFormats();
        result.setFormats(formats);
        return result;
    }

    @JsonProperty("formats")
    public Map<DisplayFormatKey, Map<String, String>> getFormats() {
        return formats;
    }

    @JsonProperty("formats")
    public void setFormats(Map<DisplayFormatKey, Map<String, String>> formats) {
        if (formats == null) {
            this.formats = Map.of();
            return;
        }

        Map<DisplayFormatKey, Map<String, String>> normalized = new EnumMap<>(DisplayFormatKey.class);
        formats.forEach((key, translations) -> {
            Map<String, String> normalizedTranslations = new LinkedHashMap<>();
            if (translations != null) {
                translations.forEach((language, pattern) -> {
                    String normalizedLanguage = LanguageUtils.normalize(language);
                    if (normalizedTranslations.putIfAbsent(normalizedLanguage, pattern) != null) {
                        throw new IllegalArgumentException(
                                "Duplicate language after normalization for display format " + key + ": " + language
                        );
                    }
                });
            }
            normalized.put(key, Collections.unmodifiableMap(normalizedTranslations));
        });
        this.formats = Collections.unmodifiableMap(normalized);
    }
}
