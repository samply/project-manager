package de.samply.form.core.condition;

import de.samply.form.core.FormConfig;
import de.samply.form.core.model.FormFieldConfig;
import de.samply.form.core.model.FormFieldType;
import de.samply.frontend.dto.FormField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormFieldConditionEvaluatorTest {

    private static final String CROSS_FORM_CONDITION =
            "['ethics']['ethics_approval_status']['value'] == 'approved'";
    private static final String BLOOD_CONDITION = "['samples']['liquid_type']['value'] == 'blood'";
    private static final String URINE_CONDITION = "['samples']['liquid_type']['value'] == 'urine'";

    private static final FormFieldConfig BLOOD_VOLUME =
            FormFieldConfig.builder().label("volume").condition(BLOOD_CONDITION).mandatory(true).build();
    private static final FormFieldConfig URINE_VOLUME =
            FormFieldConfig.builder().label("volume").condition(URINE_CONDITION).build();
    private static final FormFieldConfig DEFAULT_VOLUME = FormFieldConfig.builder().label("volume").build();

    @Test
    void showsAFieldWhoseConditionRefersToAnotherFormWhenBothFormsAreEvaluatedTogether() {
        FormFieldConditionEvaluator evaluator = new FormFieldConditionEvaluator(crossFormConfig());

        assertThat(evaluator.instanceResolver(List.of(ethicsStatus(), fundingDetails())).resolve(fundingDetails()))
                .isPresent();
    }

    @Test
    void hidesAFieldWhoseConditionRefersToAnotherFormWhenOnlyItsOwnFormIsEvaluated() {
        // Why the PDF fetches the fields of all forms in one call: evaluated
        // form by form, the referenced form is missing from the context.
        FormFieldConditionEvaluator evaluator = new FormFieldConditionEvaluator(crossFormConfig());

        assertThat(evaluator.instanceResolver(List.of(fundingDetails())).resolve(fundingDetails())).isEmpty();
    }

    @Test
    void theFirstInstanceWhoseConditionIsMetWins() {
        FormFieldConditionEvaluator evaluator =
                new FormFieldConditionEvaluator(volumeConfig(BLOOD_VOLUME, URINE_VOLUME, DEFAULT_VOLUME));

        assertThat(resolveVolume(evaluator, "urine")).contains(URINE_VOLUME);
        assertThat(resolveVolume(evaluator, "blood")).contains(BLOOD_VOLUME);
    }

    @Test
    void theLastInstanceWithoutConditionIsTheDefault() {
        FormFieldConditionEvaluator evaluator =
                new FormFieldConditionEvaluator(volumeConfig(BLOOD_VOLUME, DEFAULT_VOLUME));

        assertThat(resolveVolume(evaluator, "saliva")).contains(DEFAULT_VOLUME);
        assertThat(resolveVolume(evaluator, null)).contains(DEFAULT_VOLUME);
    }

    @Test
    void aFieldWithoutAMatchingInstanceIsNotShown() {
        FormFieldConditionEvaluator evaluator =
                new FormFieldConditionEvaluator(volumeConfig(BLOOD_VOLUME, URINE_VOLUME));

        assertThat(resolveVolume(evaluator, "saliva")).isEmpty();
    }

    @Test
    void eachBlockInstanceChoosesItsOwnInstance() {
        FormFieldConditionEvaluator evaluator =
                new FormFieldConditionEvaluator(volumeConfig(BLOOD_VOLUME, DEFAULT_VOLUME));
        FormField bloodSample = blockField("liquid_type", 1, "blood");
        FormField salivaSample = blockField("liquid_type", 2, "saliva");
        FormField volume1 = blockField("volume", 1, null);
        FormField volume2 = blockField("volume", 2, null);

        FormFieldConditionEvaluator.InstanceResolver resolver =
                evaluator.instanceResolver(List.of(bloodSample, volume1, salivaSample, volume2));

        assertThat(resolver.resolve(volume1)).contains(BLOOD_VOLUME);
        assertThat(resolver.resolve(volume2)).contains(DEFAULT_VOLUME);
    }

    private java.util.Optional<FormFieldConfig> resolveVolume(FormFieldConditionEvaluator evaluator, String liquidType) {
        FormField liquid = FormField.builder().title("samples").label("liquid_type")
                .fieldType(FormFieldType.DYNAMIC).value(liquidType).build();
        FormField volume = FormField.builder().title("samples").label("volume")
                .fieldType(FormFieldType.DYNAMIC).build();
        return evaluator.instanceResolver(List.of(liquid, volume)).resolve(volume);
    }

    private FormField blockField(String label, int blockInstance, String value) {
        return FormField.builder().title("samples").label(label).block("sample").blockInstance(blockInstance)
                .fieldType(FormFieldType.DYNAMIC).value(value).build();
    }

    private FormConfig volumeConfig(FormFieldConfig... instances) {
        FormConfig formConfig = mock(FormConfig.class);
        when(formConfig.fetchFormFieldConfigs("samples", "volume")).thenReturn(List.of(instances));
        return formConfig;
    }

    private FormConfig crossFormConfig() {
        FormConfig formConfig = mock(FormConfig.class);
        when(formConfig.fetchFormFieldConfigs("ethics", "ethics_approval_status"))
                .thenReturn(List.of(FormFieldConfig.builder().label("ethics_approval_status").build()));
        when(formConfig.fetchFormFieldConfigs("funding", "funding_details"))
                .thenReturn(List.of(FormFieldConfig.builder().label("funding_details")
                        .condition(CROSS_FORM_CONDITION).build()));
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
