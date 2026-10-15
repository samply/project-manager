package de.samply.form.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectFormField;
import de.samply.form.core.condition.FormFieldConditionEvaluator;
import de.samply.form.core.model.FormFieldConfig;
import de.samply.form.core.model.FormFieldLayout;
import de.samply.form.core.model.FormFieldLayoutRow;
import de.samply.form.core.model.FormFieldType;
import de.samply.form.core.model.FormMetadataConfig;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.ProjectAndForms;
import de.samply.project.DtoProjectService;
import de.samply.project.state.ProjectState;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
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
        Project dbProject = new Project();
        dbProject.setState(ProjectState.REVIEW);
        Form project = new Form("project", "Project", "Project description", null);
        Form query = new Form("query", "Query", "Query description", "Query short description");
        Form summary = new Form("summary", null, null, null);
        when(dtoFactory.convertForm(eq("project"), eq(language), eq(ProjectState.REVIEW), any())).thenReturn(project);
        when(dtoFactory.convertForm(eq("query"), eq(language), eq(ProjectState.REVIEW), any())).thenReturn(query);
        when(dtoFactory.convertForm(eq("summary"), eq(language), eq(ProjectState.REVIEW), any())).thenReturn(summary);

        List<Form> result = service.fetchProjectFormTitleCanonicalOrder(
                List.of("project", "query", "summary"), dbProject, language);

        assertThat(result).containsExactly(project, query, summary);
    }

    @Test
    void enrichesConfiguredInformationalFormsWithoutFieldsForTheCurrentState() {
        FormService formService = mock(FormService.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        DtoProjectService dtoProjectService = mock(DtoProjectService.class);
        DtoFormService service = new DtoFormService(
                formService, dtoFactory, mock(FormConfig.class), dtoProjectService,
                mock(FormFieldConditionEvaluator.class));
        Optional<String> language = Optional.of("en");
        Project project = new Project();
        project.setState(ProjectState.DRAFT);
        Form configuredForm = new Form("introduction", null, null, null);
        Form enrichedForm = new Form(
                "introduction", "Introduction", null, null, "Welcome", null);

        when(formService.fetchSelectedForms(project)).thenReturn(List.of());
        when(dtoProjectService.fetchCurrentProjectConfigurations(project)).thenReturn(List.of(
                new ProjectAndForms(null, new Form[]{configuredForm}, new FormField[0])));
        when(dtoFactory.convertForm(eq("introduction"), eq(language), eq(ProjectState.DRAFT), any()))
                .thenReturn(enrichedForm);

        assertThat(service.fetchSelectedForms(project, language)).containsExactly(enrichedForm);
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
        FormFieldConditionEvaluator conditionEvaluator = new FormFieldConditionEvaluator(formConfig);
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
        when(dtoFactory.convert(eq(title), eq(typeConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(formField(title, "type", 1));
        when(dtoFactory.convert(eq(title), eq(volumeConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(formField(title, "volume", 2));
        stubInstances(formConfig, title, typeConfig, volumeConfig);

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
        FormFieldConditionEvaluator conditionEvaluator = new FormFieldConditionEvaluator(formConfig);
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
        when(dtoFactory.convert(eq(title), eq(tagsConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(baseField);
        stubInstances(formConfig, title, tagsConfig);

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
        FormFieldConditionEvaluator conditionEvaluator = new FormFieldConditionEvaluator(formConfig);
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
        when(dtoFactory.convert(eq(title), eq(tagsConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(baseField);
        stubInstances(formConfig, title, tagsConfig);

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
        FormFieldConditionEvaluator conditionEvaluator = new FormFieldConditionEvaluator(formConfig);
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
        when(dtoFactory.convert(eq(title), eq(publicationConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(baseField);
        stubInstances(formConfig, title, publicationConfig);

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
        FormFieldConditionEvaluator conditionEvaluator = new FormFieldConditionEvaluator(formConfig);
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
        when(dtoFactory.convert(eq(title), eq(activeConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(formField(title, "active", null, 1, null));
        stubInstances(formConfig, title, activeConfig, inactiveConfig);

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
        FormFieldConditionEvaluator conditionEvaluator = new FormFieldConditionEvaluator(formConfig);
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
        when(dtoFactory.convert(eq(title), eq(inactiveConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(baseField);
        stubInstances(formConfig, title, inactiveConfig);

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
    void returnsInactiveFixedMetadataWithoutSendingItThroughDynamicProcessing() {
        FormService formService = mock(FormService.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        FormConfig formConfig = mock(FormConfig.class);
        DtoProjectService dtoProjectService = mock(DtoProjectService.class);
        FormFieldConditionEvaluator conditionEvaluator = new FormFieldConditionEvaluator(formConfig);
        DtoFormService service = new DtoFormService(
                formService, dtoFactory, formConfig, dtoProjectService, conditionEvaluator);

        String title = "project";
        Optional<String> language = Optional.of("en");
        Project project = new Project();
        project.setState(ProjectState.DRAFT);
        FormFieldConfig fixedConfig = formFieldConfig("PROJECT_TITLE", null);
        fixedConfig.setFieldType(FormFieldType.FIXED);
        fixedConfig.setActive(false);
        FormFieldConfig dynamicConfig = formFieldConfig("methodology", null);
        Map<String, FormFieldConfig> configuredFields = new LinkedHashMap<>();
        configuredFields.put(fixedConfig.getLabel(), fixedConfig);
        configuredFields.put(dynamicConfig.getLabel(), dynamicConfig);
        FormField fixedDto = FormField.builder()
                .title(title)
                .label("PROJECT_TITLE")
                .fieldType(FormFieldType.FIXED)
                .active(false)
                .order(1)
                .build();
        FormField dynamicDto = FormField.builder()
                .title(title)
                .label("methodology")
                .fieldType(FormFieldType.DYNAMIC)
                .order(2)
                .build();

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(Map.of(title, configuredFields));
        when(formService.fetchProjectFormFields(title, project)).thenReturn(List.of());
        when(dtoFactory.convert(eq(title), eq(fixedConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(fixedDto);
        when(dtoFactory.convert(eq(title), eq(dynamicConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(dynamicDto);
        stubInstances(formConfig, title, fixedConfig, dynamicConfig);

        Collection<FormField> result = service.fetchProjectFormFields(
                Optional.of(title), project, language);

        assertThat(result)
                .extracting(FormField::label, FormField::fieldType, FormField::active, FormField::order)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "PROJECT_TITLE", FormFieldType.FIXED, false, 1),
                        org.assertj.core.groups.Tuple.tuple(
                                "methodology", FormFieldType.DYNAMIC, null, 2));
    }

    @Test
    void showsTheMatchingInstanceWithTheStoredValue() {
        FormService formService = mock(FormService.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        FormConfig formConfig = mock(FormConfig.class);
        DtoFormService service = new DtoFormService(formService, dtoFactory, formConfig,
                mock(DtoProjectService.class), new FormFieldConditionEvaluator(formConfig));

        String title = "samples";
        Optional<String> language = Optional.empty();
        Project project = new Project();
        FormFieldConfig liquidConfig = formFieldConfig("liquid_type", null);
        FormFieldConfig bloodVolume = formFieldConfig("volume", null);
        bloodVolume.setCondition("['samples']['liquid_type']['value'] == 'blood'");
        bloodVolume.setMandatory(true);
        FormFieldConfig defaultVolume = formFieldConfig("volume", null);
        ProjectFormField storedLiquid = mock(ProjectFormField.class);
        ProjectFormField storedVolume = mock(ProjectFormField.class);
        FormField rebuiltVolume = formField(title, "volume", null, 2, "5").toBuilder().mandatory(false).build();

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(
                Map.of(title, Map.of("liquid_type", liquidConfig, "volume", bloodVolume)));
        when(formConfig.fetchFormFieldConfigs(title, "liquid_type")).thenReturn(List.of(liquidConfig));
        when(formConfig.fetchFormFieldConfigs(title, "volume")).thenReturn(List.of(bloodVolume, defaultVolume));
        when(formService.fetchProjectFormFields(title, project)).thenReturn(List.of(storedLiquid, storedVolume));
        when(dtoFactory.convert(storedLiquid, language)).thenReturn(formField(title, "liquid_type", null, 1, "saliva"));
        when(dtoFactory.convert(storedVolume, language)).thenReturn(
                formField(title, "volume", null, 2, "5").toBuilder().mandatory(true).build());
        when(dtoFactory.convert(eq(title), eq(liquidConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(formField(title, "liquid_type", null, 1, null));
        when(dtoFactory.convert(eq(title), eq(bloodVolume), any(), any(), any(), eq(language), any()))
                .thenReturn(formField(title, "volume", null, 2, null));
        when(dtoFactory.convert(eq(title), eq(defaultVolume), eq(Optional.empty()), eq(Optional.empty()),
                eq(Optional.of("5")), eq(language), any()))
                .thenReturn(rebuiltVolume);

        Collection<FormField> result = service.fetchProjectFormFields(Optional.of(title), project, language);

        assertThat(result)
                .extracting(FormField::label, FormField::value, FormField::mandatory)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("liquid_type", "saliva", null),
                        org.assertj.core.groups.Tuple.tuple("volume", "5", false));
    }

    @Test
    void hidesAMatchingInactiveInstanceUnlessTheFieldHasStoredData() {
        assertThat(fetchWithInactiveDefaultVolume(false)).extracting(FormField::label)
                .containsExactly("liquid_type");
        assertThat(fetchWithInactiveDefaultVolume(true)).extracting(FormField::label)
                .containsExactly("liquid_type", "volume");
    }

    // liquid_type is "saliva": volume matches its second, inactive instance.
    private Collection<FormField> fetchWithInactiveDefaultVolume(boolean volumeStored) {
        FormService formService = mock(FormService.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        FormConfig formConfig = mock(FormConfig.class);
        DtoFormService service = new DtoFormService(formService, dtoFactory, formConfig,
                mock(DtoProjectService.class), new FormFieldConditionEvaluator(formConfig));

        String title = "samples";
        Optional<String> language = Optional.empty();
        Project project = new Project();
        FormFieldConfig liquidConfig = formFieldConfig("liquid_type", null);
        FormFieldConfig bloodVolume = formFieldConfig("volume", null);
        bloodVolume.setCondition("['samples']['liquid_type']['value'] == 'blood'");
        FormFieldConfig inactiveVolume = formFieldConfig("volume", null);
        inactiveVolume.setActive(false);
        ProjectFormField storedLiquid = mock(ProjectFormField.class);
        ProjectFormField storedVolume = mock(ProjectFormField.class);

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(
                Map.of(title, Map.of("liquid_type", liquidConfig, "volume", bloodVolume)));
        when(formConfig.fetchFormFieldConfigs(title, "liquid_type")).thenReturn(List.of(liquidConfig));
        when(formConfig.fetchFormFieldConfigs(title, "volume")).thenReturn(List.of(bloodVolume, inactiveVolume));
        when(formService.fetchProjectFormFields(title, project)).thenReturn(
                volumeStored ? List.of(storedLiquid, storedVolume) : List.of(storedLiquid));
        when(dtoFactory.convert(storedLiquid, language)).thenReturn(formField(title, "liquid_type", null, 1, "saliva"));
        when(dtoFactory.convert(storedVolume, language)).thenReturn(formField(title, "volume", null, 2, "5"));
        when(dtoFactory.convert(eq(title), eq(liquidConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(formField(title, "liquid_type", null, 1, null));
        when(dtoFactory.convert(eq(title), eq(bloodVolume), any(), any(), any(), eq(language), any()))
                .thenReturn(formField(title, "volume", null, 2, null));
        when(dtoFactory.convert(eq(title), eq(inactiveVolume), any(), any(), any(), eq(language), any()))
                .thenReturn(formField(title, "volume", null, 2, "5"));

        return service.fetchProjectFormFields(Optional.of(title), project, language);
    }

    @Test
    void aFixedFieldShowsItsMatchingInstanceOrIsInactiveWithoutOne() {
        assertThat(fetchFixedTitle("internal"))
                .extracting(FormField::labelDisplayName, FormField::active)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Internal title", null));
        assertThat(fetchFixedTitle("external"))
                .extracting(FormField::labelDisplayName, FormField::active)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Title", false));
    }

    // PROJECT_TITLE has a single, conditional instance after its first: internal → "Internal title".
    private List<FormField> fetchFixedTitle(String type) {
        FormService formService = mock(FormService.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        FormConfig formConfig = mock(FormConfig.class);
        DtoFormService service = new DtoFormService(formService, dtoFactory, formConfig,
                mock(DtoProjectService.class), new FormFieldConditionEvaluator(formConfig));

        String title = "project";
        Optional<String> language = Optional.empty();
        Project project = new Project();
        FormFieldConfig typeConfig = formFieldConfig("type", null);
        FormFieldConfig firstTitle = formFieldConfig("PROJECT_TITLE", null);
        firstTitle.setFieldType(FormFieldType.FIXED);
        firstTitle.setCondition("['project']['type']['value'] == 'none'");
        FormFieldConfig internalTitle = formFieldConfig("PROJECT_TITLE", null);
        internalTitle.setFieldType(FormFieldType.FIXED);
        internalTitle.setCondition("['project']['type']['value'] == 'internal'");
        ProjectFormField storedType = mock(ProjectFormField.class);
        Map<String, FormFieldConfig> configuredFields = new LinkedHashMap<>();
        configuredFields.put("type", typeConfig);
        configuredFields.put("PROJECT_TITLE", firstTitle);

        when(formConfig.getFormTitleLabelFieldMap()).thenReturn(Map.of(title, configuredFields));
        when(formConfig.fetchFormFieldConfigs(title, "type")).thenReturn(List.of(typeConfig));
        when(formConfig.fetchFormFieldConfigs(title, "PROJECT_TITLE")).thenReturn(List.of(firstTitle, internalTitle));
        when(formService.fetchProjectFormFields(title, project)).thenReturn(List.of(storedType));
        when(dtoFactory.convert(storedType, language)).thenReturn(formField(title, "type", null, 1, type));
        when(dtoFactory.convert(eq(title), eq(typeConfig), any(), any(), any(), eq(language), any()))
                .thenReturn(formField(title, "type", null, 1, null));
        when(dtoFactory.convert(eq(title), eq(firstTitle), any(), any(), any(), eq(language), any()))
                .thenReturn(FormField.builder().title(title).label("PROJECT_TITLE").fieldType(FormFieldType.FIXED)
                        .labelDisplayName("Title").order(2).build());
        when(dtoFactory.convert(eq(title), eq(internalTitle), any(), any(), any(), eq(language), any()))
                .thenReturn(FormField.builder().title(title).label("PROJECT_TITLE").fieldType(FormFieldType.FIXED)
                        .labelDisplayName("Internal title").order(2).build());

        return service.fetchProjectFormFields(Optional.of(title), project, language).stream()
                .filter(field -> field.fieldType() == FormFieldType.FIXED)
                .toList();
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

    // Each config as the only instance of its label.
    private static void stubInstances(FormConfig formConfig, String title, FormFieldConfig... configs) {
        for (FormFieldConfig config : configs) {
            when(formConfig.fetchFormFieldConfigs(title, config.getLabel())).thenReturn(List.of(config));
        }
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
