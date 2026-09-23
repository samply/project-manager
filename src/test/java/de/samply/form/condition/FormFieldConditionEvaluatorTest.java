package de.samply.form.condition;

import de.samply.form.FormConfig;
import de.samply.form.FormFieldConfig;
import de.samply.form.FormFieldType;
import de.samply.frontend.dto.FormField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormFieldConditionEvaluatorTest {

    private static final String CROSS_FORM_CONDITION =
            "['ethics']['ethics_approval_status']['value'] == 'approved'";

    @Test
    void showsAFieldWhoseConditionRefersToAnotherFormWhenBothFormsAreEvaluatedTogether() {
        FormFieldConditionEvaluator evaluator = new FormFieldConditionEvaluator(formConfig());

        assertThat(evaluator.filter(List.of(ethicsStatus(), fundingDetails())))
                .extracting(FormField::label)
                .containsExactly("ethics_approval_status", "funding_details");
    }

    @Test
    void hidesAFieldWhoseConditionRefersToAnotherFormWhenOnlyItsOwnFormIsEvaluated() {
        // Why the PDF fetches the fields of all forms in one call: evaluated
        // form by form, the referenced form is missing from the context.
        FormFieldConditionEvaluator evaluator = new FormFieldConditionEvaluator(formConfig());

        assertThat(evaluator.filter(List.of(fundingDetails()))).isEmpty();
    }

    private FormConfig formConfig() {
        FormConfig formConfig = mock(FormConfig.class);
        when(formConfig.fetchFormFieldConfig("ethics", "ethics_approval_status"))
                .thenReturn(FormFieldConfig.builder().label("ethics_approval_status").build());
        when(formConfig.fetchFormFieldConfig("funding", "funding_details"))
                .thenReturn(FormFieldConfig.builder().label("funding_details")
                        .condition(CROSS_FORM_CONDITION).build());
        return formConfig;
    }

    private FormField ethicsStatus() {
        return FormField.builder().title("ethics").label("ethics_approval_status")
                .fieldType(FormFieldType.DYNAMIC).value("approved").build();
    }

    private FormField fundingDetails() {
        return FormField.builder().title("funding").label("funding_details")
                .fieldType(FormFieldType.DYNAMIC).build();
    }
}
