package de.samply.display;

import de.samply.app.ProjectManagerConst;
import de.samply.utils.LanguageUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.Instant;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DisplayFormatService {

    private static final Set<String> SUPPORTED_TOKENS = Set.of(
            "d", "dd", "M", "MM", "MMM", "MMMM", "yyyy", "HH", "mm", "ss"
    );

    private final DisplayFormats displayFormats;
    private final String defaultLanguage;
    private final DisplayFormatKey defaultDateDisplayFormat;
    private final DisplayFormatKey defaultTimestampDisplayFormat;

    public DisplayFormatService(
            DisplayFormats displayFormats,
            @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage,
            @Value(ProjectManagerConst.DEFAULT_DATE_FORM_FIELD_DISPLAY_FORMAT_SV) DisplayFormatKey defaultDateDisplayFormat,
            @Value(ProjectManagerConst.DEFAULT_TIMESTAMP_FORM_FIELD_DISPLAY_FORMAT_SV) DisplayFormatKey defaultTimestampDisplayFormat
    ) {
        this.displayFormats = displayFormats;
        this.defaultLanguage = normalizeRequiredLanguage(defaultLanguage, "default language");
        this.defaultDateDisplayFormat = defaultDateDisplayFormat;
        this.defaultTimestampDisplayFormat = defaultTimestampDisplayFormat;
        validate();
    }

    public ResolvedDisplayFormat resolve(DisplayFormatKey key, String requestedLanguage) {
        Map<String, String> translations = displayFormats.getFormats().get(key);
        if (translations == null) {
            throw new IllegalArgumentException("Missing display format key: " + key);
        }

        String normalized = LanguageUtils.normalize(requestedLanguage);
        if (normalized != null && !normalized.isBlank()) {
            String exact = translations.get(normalized);
            if (exact != null) {
                return new ResolvedDisplayFormat(normalized, exact);
            }

            int separator = normalized.indexOf('-');
            if (separator > 0) {
                String baseLanguage = normalized.substring(0, separator);
                String base = translations.get(baseLanguage);
                if (base != null) {
                    return new ResolvedDisplayFormat(baseLanguage, base);
                }
            }
        }

        return new ResolvedDisplayFormat(defaultLanguage, translations.get(defaultLanguage));
    }

    public String format(DisplayFormatKey key, TemporalAccessor value, String requestedLanguage) {
        ResolvedDisplayFormat resolved = resolve(key, requestedLanguage);
        return DateTimeFormatter.ofPattern(
                resolved.pattern(), Locale.forLanguageTag(resolved.language())
        ).format(value);
    }

    public String format(
            DisplayFormatKey key,
            Instant value,
            String requestedLanguage,
            ZoneId zone
    ) {
        return format(key, value.atZone(zone), requestedLanguage);
    }

    public DisplayFormats getDisplayFormats() {
        return displayFormats;
    }

    public String resolveLocale(String requestedLanguage) {
        Map<String, String> overrides = displayFormats.getLocales();
        if (overrides.isEmpty()) overrides = displayFormats.getLegacyNumberFormats();
        // Date-format languages define the existing deployment language fallback.
        String language = resolve(defaultDateDisplayFormat, requestedLanguage).language();
        String requested = LanguageUtils.normalize(requestedLanguage);
        if (requested != null && !requested.isBlank()) {
            if (overrides.containsKey(requested)) return overrides.get(requested);
            String base = requested.split("-")[0];
            if (overrides.containsKey(base)) return overrides.get(base);
        }
        return overrides.getOrDefault(language, language);
    }

    public String formatNumber(String value, String requestedLanguage) {
        if (value == null || !value.matches("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")) return value;
        try {
            BigDecimal number = new BigDecimal(value).stripTrailingZeros();
            // Match the browser formatter's precision range. Preserve unusual values
            // verbatim instead of silently rounding or expanding enormous exponents.
            if (number.scale() > 100 || (long) number.precision() - number.scale() > 309) return value;
            NumberFormat formatter = NumberFormat.getNumberInstance(
                    Locale.forLanguageTag(resolveLocale(requestedLanguage)));
            formatter.setGroupingUsed(true);
            formatter.setMaximumFractionDigits(100);
            formatter.setMinimumFractionDigits(0);
            return formatter.format(number);
        } catch (NumberFormatException exception) {
            return value;
        }
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public DisplayFormatKey getDefaultDateDisplayFormat() {
        return defaultDateDisplayFormat;
    }

    public DisplayFormatKey getDefaultTimestampDisplayFormat() {
        return defaultTimestampDisplayFormat;
    }

    private void validate() {
        Map<String, String> locales = displayFormats.getLocales();
        if (locales.isEmpty()) locales = displayFormats.getLegacyNumberFormats();
        locales.forEach((language, localeTag) -> {
            normalizeRequiredLanguage(language, "number-format language");
            if (localeTag == null || localeTag.isBlank()) {
                throw new IllegalArgumentException("Missing number-format locale for " + language);
            }
            try {
                Locale locale = new Locale.Builder().setLanguageTag(localeTag).build();
                boolean supported = java.util.Arrays.stream(NumberFormat.getAvailableLocales())
                        .anyMatch(available -> available.getLanguage().equals(locale.getLanguage()));
                if (!supported) throw new IllegalArgumentException("Unsupported locale");
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid number-format locale for " + language + ": " + localeTag, exception);
            }
        });
        Map<DisplayFormatKey, Map<String, String>> formats = displayFormats.getFormats();
        EnumSet<DisplayFormatKey> missingKeys = EnumSet.allOf(DisplayFormatKey.class);
        missingKeys.removeAll(formats.keySet());
        if (!missingKeys.isEmpty()) {
            throw new IllegalArgumentException("Missing required display format keys: " + missingKeys);
        }

        formats.forEach((key, translations) -> {
            if (translations == null || translations.isEmpty()) {
                throw new IllegalArgumentException("Display format has no translations: " + key);
            }
            if (!translations.containsKey(defaultLanguage)) {
                throw new IllegalArgumentException(
                        "Display format " + key + " has no translation for default language " + defaultLanguage
                );
            }
            translations.forEach((language, pattern) -> validateEntry(key, language, pattern));
        });
    }

    private void validateEntry(DisplayFormatKey key, String language, String pattern) {
        normalizeRequiredLanguage(language, "language for " + key);
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("Blank display pattern for " + key + " and language " + language);
        }
        validateTokens(key, language, pattern);
        try {
            DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(language));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid display pattern for " + key + " and language " + language + ": " + pattern,
                    exception
            );
        }
    }

    private void validateTokens(DisplayFormatKey key, String language, String pattern) {
        for (int index = 0; index < pattern.length();) {
            char current = pattern.charAt(index);
            if (!Character.isLetter(current)) {
                index++;
                continue;
            }

            int end = index + 1;
            while (end < pattern.length() && pattern.charAt(end) == current) {
                end++;
            }
            String token = pattern.substring(index, end);
            if (!SUPPORTED_TOKENS.contains(token)) {
                throw new IllegalArgumentException(
                        "Unsupported token '" + token + "' in display pattern for "
                                + key + " and language " + language
                );
            }
            index = end;
        }
    }

    private String normalizeRequiredLanguage(String language, String description) {
        String normalized = LanguageUtils.normalize(language);
        if (normalized == null || normalized.isBlank()
                || "und".equals(Locale.forLanguageTag(normalized).toLanguageTag())) {
            throw new IllegalArgumentException("Invalid " + description + ": " + language);
        }
        return normalized;
    }
}
