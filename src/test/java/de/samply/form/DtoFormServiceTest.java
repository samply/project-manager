package de.samply.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectFormField;
import de.samply.form.condition.FormFieldConditionEvaluator;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.FormField;
import de.samply.project.DtoProjectService;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DtoFormServiceTest {

    @Test
    void deserializesLayoutsFromFormMetadataConfig() throws Exception {
        FormMetadataConfig config = new ObjectMapper().readValue("""
                {
                  "title": "patient",
                  "layouts": [
                    {
                      "rows": [
                        {"fields": ["patient-id", "birth-date"]}
                      ]
                    }
                  ]
                }
                """, FormMetadataConfig.class);

        assertThat(config.getLayouts())
                .containsExactly(new FormFieldLayout(List.of(
                        new FormFieldLayoutRow(List.of("patient-id", "birth-date")))));
    }

    @Test
    void createsCompleteMinimumBlockInstancesWhenNoValuesArePersisted() {
        FormService formService = mock(FormService.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        FormConfig formConfig = mock(FormConfig.class);
        DtoProjectService dtoProjectService = mock(DtoProjectService.class);
        FormFieldConditionEvaluator conditionEvaluator = mock(FormFieldConditionEvaluator.class);
        DtoFormService service = new DtoFormService(
                formService, dtoFactory, formConfig, dtoProjectService, conditionEvaluator);

        String title = "samples";
        Optional<String> language = Optional.empty();
        Project project = new Project();
        FormFieldConfig typeConfig = formFieldConfig("type", "liquid");
        FormFieldConfig volumeConfig = formFieldConfig("volume", "liquid");

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(
                Map.of(title, Map.of("type", typeConfig, "volume", volumeConfig)));
        when(formService.fetchProjectFormFields(title, project)).thenReturn(List.of());
        when(dtoFactory.convert(eq(title), eq(typeConfig), any(), any(), eq(language)))
                .thenReturn(formField(title, "type", 1));
        when(dtoFactory.convert(eq(title), eq(volumeConfig), any(), any(), eq(language)))
                .thenReturn(formField(title, "volume", 2));
        when(conditionEvaluator.filter(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Collection<FormField> result = service.fetchProjectFormFields(
                Optional.of(title), project, language);

        assertThat(result)
                .extracting(FormField::label, FormField::blockInstance)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("type", 1),
                        org.assertj.core.groups.Tuple.tuple("volume", 1));
    }

    @Test
    void ignoresInactiveFieldWhenItHasNotBeenPersisted() {
        FormService formService = mock(FormService.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        FormConfig formConfig = mock(FormConfig.class);
        DtoProjectService dtoProjectService = mock(DtoProjectService.class);
        FormFieldConditionEvaluator conditionEvaluator = mock(FormFieldConditionEvaluator.class);
        DtoFormService service = new DtoFormService(
                formService, dtoFactory, formConfig, dtoProjectService, conditionEvaluator);

        String title = "project";
        Optional<String> language = Optional.empty();
        Project project = new Project();
        FormFieldConfig activeConfig = formFieldConfig("active", null);
        FormFieldConfig inactiveConfig = formFieldConfig("inactive", null);
        inactiveConfig.setActive(false);

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(
                Map.of(title, Map.of("active", activeConfig, "inactive", inactiveConfig)));
        when(formService.fetchProjectFormFields(title, project)).thenReturn(List.of());
        when(dtoFactory.convert(eq(title), eq(activeConfig), any(), any(), eq(language)))
                .thenReturn(formField(title, "active", null, 1, null));
        when(conditionEvaluator.filter(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Collection<FormField> result = service.fetchProjectFormFields(
                Optional.of(title), project, language);

        assertThat(result).extracting(FormField::label).containsExactly("active");
    }

    @Test
    void keepsInactiveFieldWhenItHasBeenPersisted() {
        FormService formService = mock(FormService.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        FormConfig formConfig = mock(FormConfig.class);
        DtoProjectService dtoProjectService = mock(DtoProjectService.class);
        FormFieldConditionEvaluator conditionEvaluator = mock(FormFieldConditionEvaluator.class);
        DtoFormService service = new DtoFormService(
                formService, dtoFactory, formConfig, dtoProjectService, conditionEvaluator);

        String title = "project";
        Optional<String> language = Optional.empty();
        Project project = new Project();
        FormFieldConfig inactiveConfig = formFieldConfig("inactive", null);
        inactiveConfig.setActive(false);
        ProjectFormField persistedField = new ProjectFormField();
        FormField baseField = formField(title, "inactive", null, 1, null);
        FormField valuedField = formField(title, "inactive", null, 1, "saved value");

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(
                Map.of(title, Map.of("inactive", inactiveConfig)));
        when(formService.fetchProjectFormFields(title, project)).thenReturn(List.of(persistedField));
        when(dtoFactory.convert(persistedField, language)).thenReturn(valuedField);
        when(dtoFactory.convert(eq(title), eq(inactiveConfig), any(), any(), eq(language)))
                .thenReturn(baseField);
        when(conditionEvaluator.filter(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Collection<FormField> result = service.fetchProjectFormFields(
                Optional.of(title), project, language);

        assertThat(result)
                .singleElement()
                .satisfies(field -> {
                    assertThat(field.label()).isEqualTo("inactive");
                    assertThat(field.value()).isEqualTo("saved value");
                });
    }

    @Test
    void fetchesLayoutsGroupedByFormTitle() {
        FormConfig formConfig = mock(FormConfig.class);
        FormFieldLayout patientLayout = new FormFieldLayout(List.of(new FormFieldLayoutRow(List.of("patient-id"))));
        FormFieldLayout sharedLayout = new FormFieldLayout(List.of(new FormFieldLayoutRow(List.of("field-a", "field-b"))));

        when(formConfig.getFormTitleLayoutsMap()).thenReturn(Map.of(
                "patient", List.of(patientLayout, sharedLayout),
                "administration", List.of()));
        DtoFormService service = dtoFormService(formConfig);

        assertThat(service.fetchFormLayouts(Optional.empty()))
                .containsExactly(Map.entry("patient", List.of(patientLayout, sharedLayout)));
    }

    @Test
    void filtersLayoutsByFormTitle() {
        FormConfig formConfig = mock(FormConfig.class);
        FormFieldLayout layout = new FormFieldLayout(List.of(new FormFieldLayoutRow(List.of("field-a"))));

        when(formConfig.getFormTitleLayoutsMap()).thenReturn(Map.of(
                "patient", List.of(layout),
                "sample", List.of()));
        DtoFormService service = dtoFormService(formConfig);

        assertThat(service.fetchFormLayouts(Optional.of("patient")))
                .containsExactly(Map.entry("patient", List.of(layout)));

        assertThat(service.fetchFormLayouts(Optional.of("sample"))).isEmpty();
    }

    private DtoFormService dtoFormService(FormConfig formConfig) {
        return new DtoFormService(
                mock(FormService.class),
                mock(DtoFactory.class),
                formConfig,
                mock(DtoProjectService.class),
                mock(FormFieldConditionEvaluator.class));
    }

    private FormFieldConfig formFieldConfig(String label, @SuppressWarnings("SameParameterValue") String block) {
        FormFieldConfig config = new FormFieldConfig();
        config.setLabel(label);
        config.setBlock(block);
        config.setActive(true);
        return config;
    }

    private FormField formField(String title, String label, int order) {
        return formField(title, label, "liquid", order, null).toBuilder()
                .minBlockInstances(1)
                .build();
    }

    private FormField formField(String title, String label, String block, Integer order, String value) {
        return FormField.builder()
                .title(title)
                .label(label)
                .block(block)
                .order(order)
                .value(value)
                .build();
    }
}
