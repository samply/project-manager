package de.samply.display;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayFormatServiceTest {

    @ParameterizedTest
    @CsvSource(value = {
            "1000|1,000|1.000",
            "1000.1|1,000.1|1.000,1",
            "-1000.1234567|-1,000.1234567|-1.000,1234567",
            "0|0|0",
            "-0.0|0|0",
            "1000.1000|1,000.1|1.000,1",
            "1e3|1,000|1.000",
            "1e-7|0.0000001|0,0000001",
            "12345678901234567890.1234567890123456789|12,345,678,901,234,567,890.1234567890123456789|12.345.678.901.234.567.890,1234567890123456789"
    }, delimiter = '|')
    void formatsNumbersWithoutLosingPrecision(String raw, String english, String german) {
        DisplayFormatService service = service(validFormats());
        assertThat(service.formatNumber(raw, "en")).isEqualTo(english);
        assertThat(service.formatNumber(raw, "de-DE")).isEqualTo(german);
    }

    @Test
    void preservesMissingInvalidAndOutOfRangeNumbers() {
        DisplayFormatService service = service(validFormats());
        assertThat(service.formatNumber(null, "de")).isNull();
        for (String raw : new String[]{"", " ", "1.000,1", "NaN", "Infinity", "01", "1e-101", "1e309"}) {
            assertThat(service.formatNumber(raw, "de")).isEqualTo(raw);
        }
    }

    @Test
    void resolvesOptionalNumberLocalesAndDefaultLanguage() {
        DisplayFormats formats = DisplayFormats.of(validFormats());
        formats.setLocales(Map.of("en", "en-US", "de", "de-DE", "de_CH", "de-CH"));
        DisplayFormatService service = new DisplayFormatService(formats, "de",
                DisplayFormatKey.DATE_FORMAT, DisplayFormatKey.DATE_TIME_FORMAT);
        assertThat(service.resolveLocale("de_ch")).isEqualTo("de-CH");
        assertThat(service.resolveLocale("en-GB")).isEqualTo("en-US");
        assertThat(service.resolveLocale("fr")).isEqualTo("de-DE");
        assertThat(service.resolveLocale(null)).isEqualTo("de-DE");
        assertThat(service.formatNumber("1000.1", null)).isEqualTo("1.000,1");
    }

    @Test
    void rejectsInvalidNumberConfiguration() {
        DisplayFormats formats = DisplayFormats.of(validFormats());
        for (String locale : new String[]{"", "de_DE", "xx-XX"}) {
            formats.setLocales(Map.of("en", locale));
            assertThatThrownBy(() -> new DisplayFormatService(formats, "en",
                    DisplayFormatKey.DATE_FORMAT, DisplayFormatKey.DATE_TIME_FORMAT))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("locale");
        }
        assertThatThrownBy(() -> formats.setLocales(Map.of("de-DE", "de-DE", "de_de", "de-DE")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate");
    }

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
