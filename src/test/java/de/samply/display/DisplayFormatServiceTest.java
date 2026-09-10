package de.samply.display;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayFormatServiceTest {

    @Test
    void resolvesExactBaseAndDefaultLanguages() {
        DisplayFormatService service = service(validFormats());

        assertThat(service.resolve(DisplayFormatKey.DATE_FORMAT, "de").pattern()).isEqualTo("dd.MM.yyyy");
        assertThat(service.resolve(DisplayFormatKey.DATE_FORMAT, "de-DE").language()).isEqualTo("de");
        assertThat(service.resolve(DisplayFormatKey.DATE_FORMAT, "fr").language()).isEqualTo("en");
        assertThat(service.resolve(DisplayFormatKey.DATE_FORMAT, null).language()).isEqualTo("en");
    }

    @Test
    void formatsWithTheLocaleAssociatedWithTheResolvedPattern() {
        DisplayFormatService service = service(validFormats());

        assertThat(service.format(DisplayFormatKey.LONG_DATE_FORMAT, LocalDate.of(2026, 9, 10), "de-DE"))
                .isEqualTo("10. September 2026");
        assertThat(service.format(DisplayFormatKey.LONG_DATE_FORMAT, LocalDate.of(2026, 9, 10), "fr"))
                .isEqualTo("September 10, 2026");
    }

    @Test
    void exposesConfiguredDefaultFormFieldDisplayFormats() {
        DisplayFormatService service = service(validFormats());

        assertThat(service.getDefaultDateDisplayFormat()).isEqualTo(DisplayFormatKey.DATE_FORMAT);
        assertThat(service.getDefaultTimestampDisplayFormat()).isEqualTo(DisplayFormatKey.DATE_TIME_FORMAT);
    }

    @Test
    void rejectsMissingRequiredKey() {
        Map<DisplayFormatKey, Map<String, String>> formats = validFormats();
        formats.remove(DisplayFormatKey.DATE_TIME_FORMAT);

        assertThatThrownBy(() -> service(formats))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DATE_TIME_FORMAT");
    }

    @Test
    void rejectsMissingDefaultLanguage() {
        Map<DisplayFormatKey, Map<String, String>> formats = validFormats();
        formats.put(DisplayFormatKey.DATE_FORMAT, Map.of("de", "dd.MM.yyyy"));

        assertThatThrownBy(() -> service(formats))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("default language en");
    }

    @Test
    void rejectsTokenOutsideSharedDialect() {
        Map<DisplayFormatKey, Map<String, String>> formats = validFormats();
        formats.put(DisplayFormatKey.LONG_DATE_FORMAT, Map.of("en", "EEEE, MMMM d, yyyy"));

        assertThatThrownBy(() -> service(formats))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported token 'EEEE'");
    }

    private DisplayFormatService service(Map<DisplayFormatKey, Map<String, String>> formats) {
        return new DisplayFormatService(
                DisplayFormats.of(formats), "en",
                DisplayFormatKey.DATE_FORMAT, DisplayFormatKey.DATE_TIME_FORMAT);
    }

    private Map<DisplayFormatKey, Map<String, String>> validFormats() {
        Map<DisplayFormatKey, Map<String, String>> formats = new EnumMap<>(DisplayFormatKey.class);
        formats.put(DisplayFormatKey.DATE_FORMAT, Map.of("en", "yyyy-MM-dd", "de", "dd.MM.yyyy"));
        formats.put(DisplayFormatKey.LONG_DATE_FORMAT,
                Map.of("en", "MMMM d, yyyy", "de", "d. MMMM yyyy"));
        formats.put(DisplayFormatKey.DATE_TIME_FORMAT,
                Map.of("en", "yyyy-MM-dd HH:mm", "de", "dd.MM.yyyy HH:mm"));
        formats.put(DisplayFormatKey.DATE_TIME_WITH_SECONDS_FORMAT,
                Map.of("en", "yyyy-MM-dd HH:mm:ss", "de", "dd.MM.yyyy HH:mm:ss"));
        return formats;
    }
}
