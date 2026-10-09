package de.samply.form.core;

import de.samply.form.core.model.DataType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that a form field value matches its data type. The frontend applies
 * the same rules before saving (formValueValidation.ts in project-manager-ui),
 * so keep both in sync. Blank values are not checked: whether a value is
 * required is decided when the request is created. Surrounding whitespace
 * (e.g. from pasting) does not count and is removed before saving (normalize).
 */
public final class FormFieldValueValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    // A dot as decimal separator, no grouping: the canonical, stored form.
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    // The stored forms, as shown by FormValueDisplayService: an instant in UTC
    // (seconds required, fraction optional) and a local date and time in minutes.
    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:\\.\\d{1,9})?Z$");
    private static final Pattern LOCAL_DATE_TIME_PATTERN =
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})T(?:[01]\\d|2[0-3]):[0-5]\\d$");
    // ASCII whitespace only, as trimmed by the frontend too (String.strip() would
    // differ from JavaScript's trim(), e.g. for a non-breaking space).
    private static final Pattern SURROUNDING_WHITESPACE =
            Pattern.compile("^[ \\t\\n\\r\\f\\u000B]+|[ \\t\\n\\r\\f\\u000B]+$");
    // A decimal comma ("1,5"), read as a decimal point. Not with exactly three
    // digits after it: "1,500" could also mean 1500 (thousands separator).
    private static final Pattern DECIMAL_COMMA = Pattern.compile("^-?\\d+,(?:\\d{1,2}|\\d{4,})$");

    private record Rule(Predicate<String> isValid, String message) {
    }

    // The checked data types; the others accept any value.
    private static final Map<DataType, Rule> RULES = new EnumMap<>(Map.of(
            DataType.EMAIL, new Rule(value -> EMAIL_PATTERN.matcher(value).matches(),
                    "is not a valid e-mail address"),
            DataType.INTEGER, new Rule(FormFieldValueValidator::isValidInteger,
                    "is not a valid whole number"),
            DataType.DOUBLE, new Rule(FormFieldValueValidator::isValidDouble,
                    "is not a valid number (use a dot as decimal separator, e.g. 1.5)"),
            DataType.DATE, new Rule(FormFieldValueValidator::isValidDate,
                    "is not a valid date (YYYY-MM-DD)"),
            DataType.TIMESTAMP, new Rule(value -> isValidDateTime(TIMESTAMP_PATTERN, value),
                    "is not a valid date and time (YYYY-MM-DDTHH:MM:SSZ)"),
            DataType.LOCAL_DATE_TIME, new Rule(value -> isValidDateTime(LOCAL_DATE_TIME_PATTERN, value),
                    "is not a valid date and time (YYYY-MM-DDTHH:MM)")
    ));

    private FormFieldValueValidator() {
    }

    /**
     * The reason why the value does not match the data type, or empty when it does.
     * Checked as it is saved (see normalize): e.g. " 1,5" as "1.5".
     */
    public static Optional<String> fetchInvalidValueMessage(DataType dataType, String rawValue) {
        Rule rule = dataType == null ? null : RULES.get(dataType);
        if (rule == null || rawValue == null) {
            return Optional.empty();
        }
        String value = canonicalText(dataType, rawValue);
        return value.isEmpty() || rule.isValid().test(value)
                ? Optional.empty()
                : Optional.of("\"" + value + "\" " + rule.message());
    }

    public static boolean isValid(DataType dataType, String value) {
        return fetchInvalidValueMessage(dataType, value).isEmpty();
    }

    /**
     * The value as it is saved: for the checked data types without surrounding
     * whitespace, and for DOUBLE with an unambiguous decimal comma as a point
     * ("1,5" as "1.5").
     */
    public static String normalize(DataType dataType, String value) {
        return value != null && dataType != null && RULES.containsKey(dataType)
                ? canonicalText(dataType, value) : value;
    }

    private static String canonicalText(DataType dataType, String value) {
        String trimmed = SURROUNDING_WHITESPACE.matcher(value).replaceAll("");
        return dataType == DataType.DOUBLE && DECIMAL_COMMA.matcher(trimmed).matches()
                ? trimmed.replace(',', '.') : trimmed;
    }

    private static boolean isValidInteger(String value) {
        if (!INTEGER_PATTERN.matcher(value).matches()) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isValidDouble(String value) {
        return DOUBLE_PATTERN.matcher(value).matches() && Double.isFinite(Double.parseDouble(value));
    }

    // The time is checked by the pattern (no 24:00, no leap second), the date like a DATE.
    private static boolean isValidDateTime(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.matches() && isValidDate(matcher.group(1));
    }

    private static boolean isValidDate(String value) {
        if (!DATE_PATTERN.matcher(value).matches()) {
            return false;
        }
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

}
