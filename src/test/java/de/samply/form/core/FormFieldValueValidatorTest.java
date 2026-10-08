package de.samply.form.core;

import de.samply.form.core.model.DataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

// Same cases as tests/formValueValidation.test.cjs in project-manager-ui: keep both in sync.
class FormFieldValueValidatorTest {

    @ParameterizedTest
    @CsvSource({
            "EMAIL, a@b.de",
            "EMAIL, first.last+tag@sub.dkfz-heidelberg.de",
            "EMAIL, A_B%c@x.museum",
            "INTEGER, 0",
            "INTEGER, -5",
            "INTEGER, 2147483647",
            "INTEGER, -2147483648",
            "DOUBLE, 0",
            "DOUBLE, -1.5",
            "DOUBLE, 1000.25",
            "DOUBLE, 1e-7",
            "DOUBLE, 2E10",
            "DATE, 2026-10-07",
            "DATE, 2024-02-29",
            "STRING, anything"
    })
    void acceptsValuesMatchingTheDataType(DataType dataType, String value) {
        assertThat(FormFieldValueValidator.isValid(dataType, value)).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "EMAIL | abc",
            "EMAIL | a@b",
            "EMAIL | a@b.",
            "EMAIL | @b.de",
            "EMAIL | a b@c.de",
            "EMAIL | a@b.d",
            "EMAIL | a@@b.de",
            "EMAIL | a@b..de",
            "INTEGER | 1.5",
            "INTEGER | 1,5",
            "INTEGER | abc",
            "INTEGER | 2147483648",
            "INTEGER | 1e3",
            "INTEGER | +1",
            "DOUBLE | 1.000,5",
            "DOUBLE | abc",
            "DOUBLE | .5",
            "DOUBLE | 1.",
            "DOUBLE | 1e999",
            "DATE | 2025-02-29",
            "DATE | 2026-13-01",
            "DATE | 07.10.2026",
            "DATE | 2026-1-7"
    })
    void rejectsValuesNotMatchingTheDataType(DataType dataType, String value) {
        assertThat(FormFieldValueValidator.fetchInvalidValueMessage(dataType, value))
                .hasValueSatisfying(message -> assertThat(message).startsWith("\"" + value + "\""));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void doesNotCheckBlankValues(String value) {
        assertThat(FormFieldValueValidator.isValid(DataType.EMAIL, value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "a b@c.de"})
    void rejectsInvalidEmailWithMessage(String value) {
        assertThat(FormFieldValueValidator.fetchInvalidValueMessage(DataType.EMAIL, value))
                .contains("\"" + value + "\" is not a valid e-mail address");
    }

    // Edge cases

    @ParameterizedTest
    @ValueSource(strings = {
            "a@b.de\nx",         // a line break must not slip through "$"
            "a@b.de\u00a0",      // a non-breaking space is not trimmed (as in the frontend)
            "jörg@b.de",         // non-ASCII local part
            "a@bü.de",           // internationalized domain (not punycode)
            "a@b.de1",           // TLD with a digit
            "a@-.de@b.de",       // two @
            "a,b@c.de",
            "a@b_c.de",          // underscore in the domain
            "a@.de",
            "<a@b.de>",
            "Name <a@b.de>",
            "mailto:a@b.de",
            "o'reilly@example.org"   // valid by RFC 5322, but not allowed by the pattern
    })
    void rejectsEmailEdgeCases(String value) {
        assertThat(FormFieldValueValidator.isValid(DataType.EMAIL, value)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "a@b.co",
            "A@B.DE",                     // case does not matter
            "a.b.c@d.e.f.example",
            "1@2.de",                     // digits only in the local part and the domain label
            "a-b@c-d.de",
            "x@xn--bcher-kva.de"          // punycode domain
    })
    void acceptsEmailEdgeCases(String value) {
        assertThat(FormFieldValueValidator.isValid(DataType.EMAIL, value)).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "EMAIL | ' a@b.de '",
            "INTEGER | ' 5'",
            "DOUBLE | '1.5 '",
            "DATE | ' 2026-10-07 '"
    })
    void ignoresSurroundingWhitespace(DataType dataType, String value) {
        assertThat(FormFieldValueValidator.isValid(dataType, value)).isTrue();
        assertThat(FormFieldValueValidator.isValid(dataType, "\t" + value + "\n")).isTrue();
    }

    @Test
    void messageQuotesTheValueWithoutSurroundingWhitespace() {
        assertThat(FormFieldValueValidator.fetchInvalidValueMessage(DataType.INTEGER, " 1.5 "))
                .contains("\"1.5\" is not a valid whole number");
    }

    @Test
    void normalizeTrimsOnlyTheCheckedDataTypes() {
        assertThat(FormFieldValueValidator.normalize(DataType.INTEGER, " 5\n")).isEqualTo("5");
        assertThat(FormFieldValueValidator.normalize(DataType.EMAIL, "\ta@b.de ")).isEqualTo("a@b.de");
        assertThat(FormFieldValueValidator.normalize(DataType.STRING, " free text ")).isEqualTo(" free text ");
        assertThat(FormFieldValueValidator.normalize(null, " x ")).isEqualTo(" x ");
        assertThat(FormFieldValueValidator.normalize(DataType.DATE, null)).isNull();
    }

    @Test
    void acceptsVeryLongEmailWithoutBacktrackingProblems() {
        String longLabel = "a".repeat(5_000);
        assertThat(FormFieldValueValidator.isValid(DataType.EMAIL, longLabel + "@" + longLabel + ".de")).isTrue();
        // A long almost-match must fail fast as well (no catastrophic backtracking)
        String almost = longLabel + "@" + (longLabel + ".").repeat(50) + "d";
        assertThat(FormFieldValueValidator.isValid(DataType.EMAIL, almost)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "-0, true",
            "007, true",                   // leading zeros are accepted
            "-2147483649, false",          // just below Integer.MIN_VALUE
            "99999999999999999999, false", // beyond long
            "-, false",
            "1 000, false",
            "1_000, false",
            "' 1', true",                 // surrounding whitespace is ignored
            "１２, false"                  // full-width digits are not ASCII digits
    })
    void integerEdgeCases(String value, boolean valid) {
        assertThat(FormFieldValueValidator.isValid(DataType.INTEGER, value)).isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({
            "-0.0, true",
            "1e308, true",
            "1.7976931348623157E308, true", // Double.MAX_VALUE
            "1e-400, true",                 // underflows to 0, still a number
            "1e309, false",                 // overflows to Infinity
            "NaN, false",                   // accepted by Double.parseDouble, not by the pattern
            "Infinity, false",
            "-Infinity, false",
            "1.5d, false",                  // Java suffix accepted by Double.parseDouble
            "1.5f, false",
            "0x1p3, false",                 // hexadecimal float
            "+1.5, false",
            "1e, false",
            "1e+, false",
            "'1.5 ', true",                // surrounding whitespace is ignored
            "1..5, false"
    })
    void doubleEdgeCases(String value, boolean valid) {
        assertThat(FormFieldValueValidator.isValid(DataType.DOUBLE, value)).isEqualTo(valid);
    }

    // Same cases as tests/formValueValidation.test.cjs in project-manager-ui
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1,5 | 1.5",
            "-0,25 | -0.25",
            "' 3,14 ' | 3.14",
            "1,2345 | 1.2345"
    })
    void readsAnUnambiguousDecimalCommaAsAPoint(String value, String saved) {
        assertThat(FormFieldValueValidator.isValid(DataType.DOUBLE, value)).isTrue();
        assertThat(FormFieldValueValidator.normalize(DataType.DOUBLE, value)).isEqualTo(saved);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1,500",     // could be 1.5 or 1500
            "1.000,5",
            "1,5,5",
            ",5",
            "1,",
            "1,5e3"
    })
    void decimalCommaEdgeCases(String value) {
        assertThat(FormFieldValueValidator.isValid(DataType.DOUBLE, value)).isFalse();
        assertThat(FormFieldValueValidator.normalize(DataType.DOUBLE, value)).isEqualTo(value);
    }

    @Test
    void decimalCommaOnlyForDouble() {
        assertThat(FormFieldValueValidator.isValid(DataType.INTEGER, "1,5")).isFalse();
        assertThat(FormFieldValueValidator.normalize(DataType.STRING, "1,5")).isEqualTo("1,5");
    }

    @ParameterizedTest
    @CsvSource({
            "2000-02-29, true",   // divisible by 400: leap year
            "1900-02-29, false",  // divisible by 100: no leap year
            "2026-02-30, false",
            "2026-04-31, false",
            "2026-00-10, false",
            "2026-01-00, false",
            "2026-12-31, true",
            "0001-01-01, true",
            "0099-12-31, true",   // two-digit years are not shifted to the 1900s
            "9999-12-31, true",
            "+10000-01-01, false",
            "2026-10-07T00:00, false",
            "2026/10/07, false",
            "20261007, false"
    })
    void dateEdgeCases(String value, boolean valid) {
        assertThat(FormFieldValueValidator.isValid(DataType.DATE, value)).isEqualTo(valid);
    }

    // The stored form, as sent by the frontend (toISOString) and shown by FormValueDisplayService
    @ParameterizedTest
    @CsvSource({
            "2026-10-07T08:30:00Z, true",
            "2026-10-07T08:30:00.000Z, true",
            "2026-10-07T08:30:00.123456789Z, true",
            "2024-02-29T23:59:59Z, true",
            "0001-01-01T00:00:00Z, true",
            "' 2026-10-07T08:30:00Z ', true",   // surrounding whitespace is ignored
            "2026-10-07T08:30Z, false",          // seconds are required
            "2026-10-07T08:30:00, false",        // no zone
            "2026-10-07T08:30:00+02:00, false",  // only UTC (Z)
            "2026-10-07T08:30:00z, false",
            "2026-10-07 08:30:00Z, false",
            "2026-10-07T24:00:00Z, false",
            "2026-10-07T08:60:00Z, false",
            "2026-10-07T23:59:60Z, false",       // leap second, accepted by Instant.parse
            "2026-10-07T8:30:00Z, false",
            "2026-10-07T08:30:00.1234567890Z, false",
            "2025-02-29T08:30:00Z, false",
            "2026-02-30T08:30:00Z, false",
            "2026-10-07, false",
            "1759825800000, false",              // epoch millis
            "07.10.2026 08:30, false"
    })
    void timestampEdgeCases(String value, boolean valid) {
        assertThat(FormFieldValueValidator.isValid(DataType.TIMESTAMP, value)).isEqualTo(valid);
    }

    // The stored form, as entered in an <input type="datetime-local"> and shown by FormValueDisplayService
    @ParameterizedTest
    @CsvSource({
            "2026-10-07T08:30, true",
            "2024-02-29T00:00, true",
            "2026-12-31T23:59, true",
            "' 2026-10-07T08:30\t', true",       // surrounding whitespace is ignored
            "2026-10-07T08:30:00, false",        // minutes only
            "2026-10-07T08:30Z, false",          // no zone
            "2026-10-07 08:30, false",
            "2026-10-07T24:00, false",
            "2026-10-07T08:60, false",
            "2026-10-07T8:30, false",
            "2025-02-29T08:30, false",
            "2026-04-31T08:30, false",
            "2026-10-07, false",
            "07.10.2026 08:30, false"
    })
    void localDateTimeEdgeCases(String value, boolean valid) {
        assertThat(FormFieldValueValidator.isValid(DataType.LOCAL_DATE_TIME, value)).isEqualTo(valid);
    }

    @Test
    void dateTimeMessagesNameTheExpectedForm() {
        assertThat(FormFieldValueValidator.fetchInvalidValueMessage(DataType.TIMESTAMP, "2026-10-07T08:30"))
                .contains("\"2026-10-07T08:30\" is not a valid date and time (YYYY-MM-DDTHH:MM:SSZ)");
        assertThat(FormFieldValueValidator.fetchInvalidValueMessage(DataType.LOCAL_DATE_TIME, "2026-10-07"))
                .contains("\"2026-10-07\" is not a valid date and time (YYYY-MM-DDTHH:MM)");
    }

    @ParameterizedTest
    @EnumSource(value = DataType.class, names = {"STRING", "LONG_STRING", "ENUM", "BOOLEAN"})
    void typesWithoutRulesAcceptAnything(DataType dataType) {
        assertThat(FormFieldValueValidator.isValid(dataType, "anything @ 1,5 / 07.10.2026")).isTrue();
    }

    @Test
    void withoutDataTypeEverythingIsValid() {
        assertThat(FormFieldValueValidator.isValid(null, "not-an-email")).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "INTEGER | 1.5     | \"1.5\" is not a valid whole number",
            "DOUBLE  | 1,500   | \"1,500\" is not a valid number",
            "DATE    | 1.1.26  | \"1.1.26\" is not a valid date (YYYY-MM-DD)"
    })
    void messagesNameTheValueAndTheExpectedFormat(DataType dataType, String value, String message) {
        assertThat(FormFieldValueValidator.fetchInvalidValueMessage(dataType, value)).contains(message);
    }

    @ParameterizedTest
    @EnumSource(DataType.class)
    void everyDataTypeIsHandled(DataType dataType) {
        // A new DataType must not break the switch: blank stays valid, no exception for any value
        assertThat(FormFieldValueValidator.isValid(dataType, "")).isTrue();
        assertThat(FormFieldValueValidator.fetchInvalidValueMessage(dataType, "x")).isNotNull();
    }

}
