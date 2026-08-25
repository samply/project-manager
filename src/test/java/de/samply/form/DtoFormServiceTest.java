package de.samply.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectFormField;
import de.samply.form.condition.FormFieldConditionEvaluator;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
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
    void fetchesEnrichedAndUnknownFormTitlesInConfiguredOrder() {
        DtoFactory dtoFactory = mock(DtoFactory.class);
        DtoFormService service = new DtoFormService(
                mock(FormService.class), dtoFactory, mock(FormConfig.class),
                mock(DtoProjectService.class), mock(FormFieldConditionEvaluator.class));
        Optional<String> language = Optional.of("en");
        Form project = new Form("project", "Project", "Project description", null);
        Form query = new Form("query", "Query", "Query description", "Query short description");
        Form summary = new Form("summary", null, null, null);
        when(dtoFactory.convertForm("project", language)).thenReturn(project);
        when(dtoFactory.convertForm("query", language)).thenReturn(query);
        when(dtoFactory.convertForm("summary", language)).thenReturn(summary);

        List<Form> result = service.fetchProjectFormTitleCanonicalOrder(
                List.of("project", "query", "summary"), language);

        assertThat(result).containsExactly(project, query, summary);
    }

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
        when(dtoFactory.convert(eq(title), eq(typeConfig), any(), any(), any(), eq(language)))
                .thenReturn(formField(title, "type", 1));
        when(dtoFactory.convert(eq(title), eq(volumeConfig), any(), any(), any(), eq(language)))
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
    void expandsMultipleFieldWithSeveralSavedValues() {
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
        FormFieldConfig tagsConfig = formFieldConfig("tags", null);
        // mock(), not new ProjectFormField() - two blank real instances would be
        // equals() to each other (Lombok @Data), and Mockito's default
        // equals-based argument matching would then let the second when(...)
        // stub silently shadow the first. A mock has identity equals instead.
        ProjectFormField persistedTag1 = mock(ProjectFormField.class);
        ProjectFormField persistedTag2 = mock(ProjectFormField.class);
        FormField baseField = formField(title, "tags", null, 1, null).toBuilder().multiple(true).build();
        FormField valuedTag2 = baseField.toBuilder().fieldInstance(2).value("b").build();
        FormField valuedTag1 = baseField.toBuilder().fieldInstance(1).value("a").build();

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(
                Map.of(title, Map.of("tags", tagsConfig)));
        when(formService.fetchProjectFormFields(title, project))
                .thenReturn(List.of(persistedTag2, persistedTag1)); // deliberately out of order
        when(dtoFactory.convert(persistedTag1, language)).thenReturn(valuedTag1);
        when(dtoFactory.convert(persistedTag2, language)).thenReturn(valuedTag2);
        when(dtoFactory.convert(eq(title), eq(tagsConfig), any(), any(), any(), eq(language)))
                .thenReturn(baseField);
        when(conditionEvaluator.filter(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Collection<FormField> result = service.fetchProjectFormFields(
                Optional.of(title), project, language);

        // Both saved values come back, sorted by fieldInstance regardless of
        // persistence order - and the generic blank base is NOT also present.
        assertThat(result)
                .extracting(FormField::fieldInstance, FormField::value)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "a"),
                        org.assertj.core.groups.Tuple.tuple(2, "b"));
    }

    @Test
    void createsOneBlankInstanceForMultipleFieldWithNoSavedValues() {
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
        FormFieldConfig tagsConfig = formFieldConfig("tags", null);
        FormField baseField = formField(title, "tags", null, 1, null).toBuilder().multiple(true).build();

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(
                Map.of(title, Map.of("tags", tagsConfig)));
        when(formService.fetchProjectFormFields(title, project)).thenReturn(List.of());
        when(dtoFactory.convert(eq(title), eq(tagsConfig), any(), any(), any(), eq(language)))
                .thenReturn(baseField);
        when(conditionEvaluator.filter(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Collection<FormField> result = service.fetchProjectFormFields(
                Optional.of(title), project, language);

        assertThat(result)
                .singleElement()
                .satisfies(field -> {
                    assertThat(field.fieldInstance()).isEqualTo(1);
                    assertThat(field.value()).isNull();
                });
    }

    @Test
    void expandsMultipleFieldIndependentlyPerBlockInstance() {
        // The scenario that would throw IllegalStateException: Duplicate key
        // under the old (pre-multiple) expandBlock grouping: a multiple field
        // ("publication") inside a multiple block ("collaborator"), with a
        // different number of values saved per block instance. field_instance
        // is scoped WITHIN block_instance, so "1" legitimately appears twice
        // here (once per block instance) - see the scoping note on
        // ProjectFormField.fieldInstance.
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
        FormFieldConfig publicationConfig = formFieldConfig("publication", "collaborator");
        FormField baseField = formField(title, "publication", "collaborator", 1, null)
                .toBuilder().multiple(true).build();

        // mock(), not new ProjectFormField() - see the comment in
        // expandsMultipleFieldWithSeveralSavedValues for why.
        ProjectFormField block1Publication1 = mock(ProjectFormField.class);
        ProjectFormField block1Publication2 = mock(ProjectFormField.class);
        ProjectFormField block2Publication1 = mock(ProjectFormField.class);

        FormField valuedBlock1Publication1 = baseField.toBuilder().blockInstance(1).fieldInstance(1).value("Paper A").build();
        FormField valuedBlock1Publication2 = baseField.toBuilder().blockInstance(1).fieldInstance(2).value("Paper B").build();
        FormField valuedBlock2Publication1 = baseField.toBuilder().blockInstance(2).fieldInstance(1).value("Paper C").build();

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(
                Map.of(title, Map.of("publication", publicationConfig)));
        when(formService.fetchProjectFormFields(title, project))
                .thenReturn(List.of(block1Publication1, block1Publication2, block2Publication1));
        when(dtoFactory.convert(block1Publication1, language)).thenReturn(valuedBlock1Publication1);
        when(dtoFactory.convert(block1Publication2, language)).thenReturn(valuedBlock1Publication2);
        when(dtoFactory.convert(block2Publication1, language)).thenReturn(valuedBlock2Publication1);
        when(dtoFactory.convert(eq(title), eq(publicationConfig), any(), any(), any(), eq(language)))
                .thenReturn(baseField);
        when(conditionEvaluator.filter(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Collection<FormField> result = service.fetchProjectFormFields(
                Optional.of(title), project, language);

        assertThat(result)
                .extracting(FormField::blockInstance, FormField::fieldInstance, FormField::value)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, 1, "Paper A"),
                        org.assertj.core.groups.Tuple.tuple(1, 2, "Paper B"),
                        org.assertj.core.groups.Tuple.tuple(2, 1, "Paper C"));
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
        when(dtoFactory.convert(eq(title), eq(activeConfig), any(), any(), any(), eq(language)))
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
        when(dtoFactory.convert(eq(title), eq(inactiveConfig), any(), any(), any(), eq(language)))
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
