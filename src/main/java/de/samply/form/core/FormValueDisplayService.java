package de.samply.form.core;

import de.samply.display.DisplayFormatKey;
import de.samply.display.DisplayFormatService;
import de.samply.form.core.model.DataType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/** Formats canonical values for display only; never rewrites stored or API values. */
@Service
public class FormValueDisplayService {

    private final DisplayFormatService displayFormatService;

    public FormValueDisplayService(DisplayFormatService displayFormatService) {
        this.displayFormatService = displayFormatService;
    }

    public String format(DataType dataType, String value, String language) {
        return format(dataType, value, language, null);
    }

    public String format(DataType dataType, String value, String language, DisplayFormatKey displayFormat) {
        if (dataType == null || value == null || value.isBlank()) return value;
        DisplayFormatKey key = displayFormat != null ? displayFormat : defaultDisplayFormat(dataType);
        try {
            return switch (dataType) {
                case INTEGER, DOUBLE -> displayFormatService.formatNumber(value, language);
                case DATE -> value.matches("\\d{4}-\\d{2}-\\d{2}")
                        ? displayFormatService.format(
                                key, LocalDate.parse(value), language)
                        : value;
                case TIMESTAMP -> value.matches(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?Z")
                        ? displayFormatService.format(
                                key, Instant.parse(value), language, ZoneOffset.UTC)
                        : value;
                case LOCAL_DATE_TIME -> value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")
                        ? displayFormatService.format(
                                key, LocalDateTime.parse(value), language)
                        : value;
                default -> value;
            };
        } catch (DateTimeParseException exception) {
            // Older or invalid values remain visible as originally stored.
            return value;
        }
    }

    /** The display format key applied when a field has no explicit display_format. Null for non-temporal (or unset) data types. */
    public DisplayFormatKey defaultDisplayFormat(DataType dataType) {
        if (dataType == null) return null;
        return switch (dataType) {
            case DATE -> displayFormatService.getDefaultDateDisplayFormat();
            case TIMESTAMP, LOCAL_DATE_TIME -> displayFormatService.getDefaultTimestampDisplayFormat();
            default -> null;
        };
    }
}
