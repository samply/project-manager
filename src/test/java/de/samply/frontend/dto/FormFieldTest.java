package de.samply.frontend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormFieldTest {

    @Test
    void fetchDisplayValueReturnsTheMatchingAllowedValuesDisplayName() {
        FormField field = enumField("plasma", new FormFieldValue("plasma", "Plasma", null, null));

        assertThat(field.fetchDisplayValue()).isEqualTo("Plasma");
    }

    @Test
    void fetchDisplayValueFallsBackToTheRawValueWhenNoAllowedValueMatches() {
        FormField field = enumField("unknown", new FormFieldValue("plasma", "Plasma", null, null));

        assertThat(field.fetchDisplayValue()).isEqualTo("unknown");
    }

    @Test
    void fetchDisplayValueUsesPresentationValueForNonEnumFields() {
        FormField field = FormField.builder()
                .value("2026-09-11")
                .displayValue("11.09.2026")
                .build();

        assertThat(field.fetchDisplayValue()).isEqualTo("11.09.2026");
    }

    @Test
    void allowedValueDisplayNameTakesPrecedenceOverPresentationValue() {
        FormField field = FormField.builder()
                .value("plasma")
                .displayValue("must not be used")
                .allowedValues(new FormFieldValue[]{new FormFieldValue("plasma", "Plasma", null, null)})
                .build();

        assertThat(field.fetchDisplayValue()).isEqualTo("Plasma");
    }

    @Test
    void presentationValueIsNotExposedInTheFrontendDto() throws Exception {
        FormField field = FormField.builder()
                .value("2026-09-11")
                .displayValue("11.09.2026")
                .build();

        String json = new ObjectMapper().writeValueAsString(field);

        assertThat(json)
                .contains("\"value\":\"2026-09-11\"")
                .doesNotContain("displayValue")
                .doesNotContain("11.09.2026");
    }

    @Test
    void fetchDisplayDescriptionReturnsTheMatchingAllowedValuesDescription() {
        FormField field = enumField("plasma",
                new FormFieldValue("plasma", "Plasma", "The fluid component of blood.", null));

        assertThat(field.fetchDisplayDescription()).isEqualTo("The fluid component of blood.");
    }

    @Test
    void fetchDisplayDescriptionIsNullWhenTheMatchingAllowedValueHasNoDescription() {
        // Regression test: a null description used to throw a NullPointerException
        // (Stream.findFirst() rejects a found null element), crashing PDF
        // generation for any ENUM field whose selected value has no description.
        FormField field = enumField("ml", new FormFieldValue("ml", "ml", null, null));

        assertThat(field.fetchDisplayDescription()).isNull();
    }

    @Test
    void fetchDisplayDescriptionIsNullWhenNoAllowedValueMatches() {
        FormField field = enumField("unknown",
                new FormFieldValue("plasma", "Plasma", "The fluid component of blood.", null));

        assertThat(field.fetchDisplayDescription()).isNull();
    }

    @Test
    void fetchDisplayDescriptionIsNullWhenThereAreNoAllowedValuesAtAll() {
        FormField field = FormField.builder().value("plasma").allowedValues(null).build();

        assertThat(field.fetchDisplayDescription()).isNull();
    }

    private static FormField enumField(String value, FormFieldValue... allowedValues) {
        return FormField.builder()
                .value(value)
                .allowedValues(allowedValues)
                .build();
    }
}
