package de.samply.form.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import de.samply.db.model.Project;
import de.samply.display.DisplayFormatKey;
import de.samply.display.DisplayFormatService;
import de.samply.form.DtoFormService;
import de.samply.form.FormConfig;
import de.samply.form.FormFieldConfig;
import de.samply.form.FormFieldLayout;
import de.samply.form.FormFieldLayoutRow;
import de.samply.form.FormFieldType;
import de.samply.form.condition.FormFieldConditionEvaluator;
import de.samply.form.pdf.FormPdfGeneratorFactory;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.FormTemplate;
import de.samply.pdf.PdfGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormTemplateServiceTest {

    @Test
    void offersEveryConfiguredTemplateSortedByTemplateId() {
        // form_titles no longer decides which templates apply: every template
        // is offered, whatever forms the project selected.
        DtoFormService dtoFormService = mock(DtoFormService.class);
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        Project project = new Project();
        Optional<String> language = Optional.of("en");
        FormTemplateMetadata samples = metadata("samples");
        FormTemplateMetadata clinicalData = metadata("clinical-data");
        FormTemplate samplesTemplate = new FormTemplate("samples", "Samples");
        FormTemplate clinicalDataTemplate = new FormTemplate("clinical-data", "Clinical data");
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        when(config.getTemplateMetadataMap()).thenReturn(Map.of(
                "samples", samples, "clinical-data", clinicalData));
        when(dtoFactory.convert(samples, language)).thenReturn(samplesTemplate);
        when(dtoFactory.convert(clinicalData, language)).thenReturn(clinicalDataTemplate);
        FormTemplateService service = new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config, dtoFactory,
                mock(DisplayFormatService.class), mock(ProjectContextFactory.class), new ProjectConfigurations(), mock(FormConfig.class), conditionEvaluator());

        assertThat(service.fetchTemplates(project, language))
                .containsExactly(clinicalDataTemplate, samplesTemplate);
        verify(dtoFormService, never()).fetchSelectedForms(any(), any());
    }

    @Test
    void rejectsTheRemovedFormTitlesKey() {
        // Removed together with template selection by form: a leftover key in
        // a deployment's config must fail loudly instead of being ignored.
        assertThatThrownBy(() -> new ObjectMapper().readValue("""
                {
                  "template": "project-overview",
                  "form_titles": ["patient", "sample"]
                }
                """, FormTemplateMetadata.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }

    @Test
    void printsTheFieldsOfEverySelectedFormFetchedInOneCall() {
        // One call for all forms (like the frontend), so conditions see every
        // form's fields; then restricted to the forms the project selected.
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata();
        selectForms(dtoFormService, project, "patient", "ethics");
        when(dtoFormService.fetchProjectFormFields(Optional.empty(), project, Optional.of("en")))
                .thenReturn(List.of(
                        dynamicField("patient", "diagnosis"),
                        dynamicField("ethics", "ethics_approval_status"),
                        dynamicField("sample", "volume")));
        FormTemplateService service = formTemplateService(dtoFormService, metadata);

        Map<String, FormField> result = service.fetchFormFields(
                project, metadata.getTemplate(), "en", mock(ProjectContext.class));

        assertThat(result.values()).extracting(FormField::label)
                .containsExactlyInAnyOrder("diagnosis", "ethics_approval_status");
        verify(dtoFormService, never()).fetchProjectFormFields(
                eq(Optional.of("patient")), any(), any());
    }

    @Test
    void excludesFixedFrontendMetadataFromTemplateFieldValues() {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata();
        selectForms(dtoFormService, project, "patient");
        FormField fixed = FormField.builder()
                .title("patient")
                .label("PROJECT_TITLE")
                .fieldType(FormFieldType.FIXED)
                .order(1)
                .build();
        FormField dynamic = FormField.builder()
                .title("patient")
                .label("diagnosis")
                .fieldType(FormFieldType.DYNAMIC)
                .order(2)
                .value("example")
                .build();
        when(dtoFormService.fetchProjectFormFields(
                Optional.empty(), project, Optional.of("en")))
                .thenReturn(List.of(fixed, dynamic));
        FormTemplateService service = formTemplateService(dtoFormService, metadata);

        Map<String, FormField> result = service.fetchFormFields(
                project, metadata.getTemplate(), "en", mock(ProjectContext.class));

        assertThat(result.values()).extracting(FormField::label).containsExactly("diagnosis");
    }

    @Test
    void filtersOutProjectFieldsMarkedInactive() {
        // A project field configured "active": false is "hidden in the form
        // field configuration" the same way an inactive FIXED/DYNAMIC field
        // is, as opposed to a field only hidden by a frontend-only role/UI
        // rule - see 2026-09-07-plan-pdf-form-field-parity.md point 3.
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata();
        metadata.setProjectFields(new FormTemplateFieldConfig[]{
                FormTemplateFieldConfig.builder().label("visible-field").active(true)
                        .projectValue("${project-code}").build(),
                FormTemplateFieldConfig.builder().label("hidden-field").active(false)
                        .projectValue("${ethics-votes-per-site}").build()
        });
        DtoFactory dtoFactory = mock(DtoFactory.class);
        when(dtoFactory.convert(anyString(), any(FormFieldConfig.class), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    FormFieldConfig config = invocation.getArgument(1);
                    return FormField.builder()
                            .title(invocation.getArgument(0))
                            .label(config.getLabel())
                            .fieldType(FormFieldType.DYNAMIC)
                            .build();
                });
        when(dtoFormService.fetchProjectFormFields(any(), eq(project), eq(Optional.of("en"))))
                .thenReturn(List.of());
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        when(config.getTemplate(metadata.getTemplate())).thenReturn(Optional.of(metadata));
        when(config.fetchProjectFormFieldTitle(metadata.getTemplate())).thenReturn("project-fields");
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        FormTemplateService service = new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config,
                dtoFactory, mock(DisplayFormatService.class), mock(ProjectContextFactory.class), new ProjectConfigurations(), mock(FormConfig.class), conditionEvaluator());

        Map<String, FormField> result = service.fetchFormFields(
                project, metadata.getTemplate(), "en", new ProjectContext(Map.of()));

        assertThat(result.values()).extracting(FormField::label).containsExactly("visible-field");
    }

    @Test
    void ordersFieldsByTheDeploymentsCanonicalSectionOrderNotAlphabeticallyByTitle() {
        // Point 1, 2026-09-09 feedback: the PDF must group fields in the same
        // section order as the frontend Summary (frontendProjectConfigurations'
        // formTitleOrder), not alphabetically by title string. "query" sorts
        // after "project" alphabetically but is configured to come first here.
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata();
        // Selected in the "wrong" (alphabetical) order on purpose, to prove
        // the order comes from the canonical section order alone.
        selectForms(dtoFormService, project, "project", "query");
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        when(config.getTemplate(metadata.getTemplate())).thenReturn(Optional.of(metadata));
        when(config.isProjectFormFieldTitle(anyString())).thenReturn(false);
        FormField projectField = FormField.builder()
                .title("project").label("methodology").fieldType(FormFieldType.DYNAMIC).build();
        FormField queryField = FormField.builder()
                .title("query").label("cohort").fieldType(FormFieldType.DYNAMIC).build();
        when(dtoFormService.fetchProjectFormFields(Optional.empty(), project, Optional.of("en")))
                .thenReturn(List.of(projectField, queryField));
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        ProjectConfigurations canonicalOrder = new ProjectConfigurations();
        canonicalOrder.setFormTitleOrder(List.of("query", "project"));
        FormTemplateService service = new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config,
                mock(DtoFactory.class), mock(DisplayFormatService.class), mock(ProjectContextFactory.class), canonicalOrder, mock(FormConfig.class), conditionEvaluator());

        Map<String, FormField> result = service.fetchFormFields(
                project, metadata.getTemplate(), "en", new ProjectContext(Map.of()));

        assertThat(result.values()).extracting(FormField::title).containsExactly("query", "project");
    }

    @Test
    void theTemplatesFormTitlesInOrderComesFirstAndTheOtherFormsKeepTheNormalOrder() {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata();
        // "samples" is not printed (not selected): it is skipped.
        metadata.setFormTitlesInOrder(new String[]{"funding", "samples", "project"});
        selectForms(dtoFormService, project, "query", "project", "ethics", "funding");
        when(dtoFormService.fetchProjectFormFields(Optional.empty(), project, Optional.of("en"))).thenReturn(List.of(
                dynamicField("query", "cohort", 1), dynamicField("project", "title", 1),
                dynamicField("ethics", "vote", 1), dynamicField("funding", "budget", 1)));
        ProjectConfigurations order = new ProjectConfigurations();
        order.setFormTitleOrder(List.of("query", "project", "ethics", "funding"));
        FormTemplateService service = projectFieldsService(dtoFormService, metadata, order);

        List<String> sections = service.fetchPlacedFields(project, metadata.getTemplate(), "en", new ProjectContext(Map.of()))
                .stream().map(FormTemplateFieldPlacement.PlacedField::section).toList();

        assertThat(sections).containsExactly("funding", "project", "query", "ethics");
    }

    @Test
    void fallsBackToTheLinkedFixedFieldsMetadataWhenTheProjectFieldsOwnIsBlank() {
        // Point 2, 2026-09-09 feedback: a project field can link to a FIXED
        // field's native key via its own "label", and defer to that FIXED
        // field's configured display_name/description when its own is blank
        // (an explicit "" counts as blank, not just a missing key - this is
        // exactly how the user configured ETHICS_VOTE_FOR_ALL_SITES).
        // Precedence: PDF template metadata > form-field metadata > default.
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata();
        metadata.setProjectFields(new FormTemplateFieldConfig[]{
                // Blank display_name entirely omitted; description explicitly
                // blank - both must fall back to the linked FIXED field's.
                FormTemplateFieldConfig.builder().label("ETHICS_VOTE_FOR_ALL_SITES")
                        .description(Map.of("en", ""))
                        .projectValue("${ethics-vote-for-all-sites-filename}").build(),
                // Has its own non-blank display_name - must NOT be overridden
                // by the (also configured) FIXED field's.
                FormTemplateFieldConfig.builder().label("QUERY_FORMAT")
                        .displayName(Map.of("en", "Query format (PDF override)"))
                        .projectValue("${query-format}").build()
        });
        FormConfig formConfig = mock(FormConfig.class);
        when(formConfig.fetchFixedFieldConfig("ETHICS_VOTE_FOR_ALL_SITES")).thenReturn(Optional.of(
                FormFieldConfig.builder()
                        .label("ETHICS_VOTE_FOR_ALL_SITES")
                        .fieldType(FormFieldType.FIXED)
                        .description(Map.of("en", "Upload ethics vote file (optional)"))
                        .build()));
        when(formConfig.fetchFixedFieldConfig("QUERY_FORMAT")).thenReturn(Optional.of(
                FormFieldConfig.builder()
                        .label("QUERY_FORMAT")
                        .fieldType(FormFieldType.FIXED)
                        .displayName(Map.of("en", "Query format (form-field metadata)"))
                        .build()));
        DtoFactory dtoFactory = mock(DtoFactory.class);
        ArgumentCaptor<FormFieldConfig> convertedConfig = ArgumentCaptor.forClass(FormFieldConfig.class);
        when(dtoFactory.convert(anyString(), convertedConfig.capture(), any(), any(), any(), any()))
                .thenAnswer(invocation -> FormField.builder()
                        .title(invocation.getArgument(0))
                        .label(((FormFieldConfig) invocation.getArgument(1)).getLabel())
                        .fieldType(FormFieldType.DYNAMIC)
                        .build());
        when(dtoFormService.fetchProjectFormFields(any(), eq(project), eq(Optional.of("en"))))
                .thenReturn(List.of());
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        when(config.getTemplate(metadata.getTemplate())).thenReturn(Optional.of(metadata));
        when(config.fetchProjectFormFieldTitle(metadata.getTemplate())).thenReturn("project-fields");
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        FormTemplateService service = new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config,
                dtoFactory, mock(DisplayFormatService.class), mock(ProjectContextFactory.class), new ProjectConfigurations(), formConfig, conditionEvaluator());

        service.fetchFormFields(project, metadata.getTemplate(), "en", new ProjectContext(Map.of()));

        Map<String, FormFieldConfig> byLabel = convertedConfig.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(FormFieldConfig::getLabel, c -> c));
        assertThat(byLabel.get("ETHICS_VOTE_FOR_ALL_SITES").getDescription())
                .containsEntry("en", "Upload ethics vote file (optional)");
        assertThat(byLabel.get("QUERY_FORMAT").getDisplayName())
                .containsEntry("en", "Query format (PDF override)");
    }

    @Test
    void putsTheFormFieldLayoutsOfEverySelectedFormIntoThePdfContext() throws Exception {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata();
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        when(config.getTemplate(metadata.getTemplate())).thenReturn(Optional.of(metadata));
        when(config.fetchTemplateFile(metadata.getTemplate())).thenReturn(Optional.of("request"));
        when(config.fetchAllFormVariables(metadata.getTemplate(), "en")).thenReturn(Map.of());
        ProjectContextFactory projectContextFactory = mock(ProjectContextFactory.class);
        when(projectContextFactory.createProjectContext(project, "en"))
                .thenReturn(new ProjectContext(Map.of()));
        FormFieldLayoutRow patientRow = new FormFieldLayoutRow(List.of("first_name", "last_name"));
        FormFieldLayoutRow sampleRow = new FormFieldLayoutRow(List.of("volume", "unit"));
        FormFieldLayoutRow unselectedRow = new FormFieldLayoutRow(List.of("dose", "dose_unit"));
        selectForms(dtoFormService, project, "patient", "sample");
        when(dtoFormService.fetchFormLayouts(Optional.empty())).thenReturn(Map.of(
                "patient", List.of(new FormFieldLayout(List.of(patientRow))),
                "sample", List.of(new FormFieldLayout(List.of(sampleRow))),
                "treatment", List.of(new FormFieldLayout(List.of(unselectedRow)))));
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        PdfGenerator pdfGenerator = mock(PdfGenerator.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(pdfGenerator);
        DisplayFormatService displayFormatService = mock(DisplayFormatService.class);
        when(displayFormatService.format(
                eq(DisplayFormatKey.LONG_DATE_FORMAT), any(Instant.class), eq("en"), eq(ZoneId.of("UTC"))))
                .thenReturn("September 10, 2026");
        FormTemplateService service = new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config,
                mock(DtoFactory.class), displayFormatService, projectContextFactory, new ProjectConfigurations(), mock(FormConfig.class), conditionEvaluator());

        service.createFormAsPdf(project, metadata.getTemplate(), Optional.of("en"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> contextCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(pdfGenerator).generatePdf(anyString(), contextCaptor.capture());
        assertThat(contextCaptor.getValue())
                .containsEntry(FormContextKey.CURRENT_DATE.getText(), "September 10, 2026")
                .containsEntry(FormContextKey.LAYOUTS.getText(), Map.of(
                        "patient", List.of(patientRow),
                        "sample", List.of(sampleRow)));
    }

    @Test
    void offersOnlyTemplatesWhoseConditionHolds() {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata ethicsReport = metadata("ethics-report");
        ethicsReport.setCondition("['ethics']['ethics_approval_status']['value'] == 'approved'");
        FormTemplateService service = service(dtoFormService, ethicsReport, metadata("request"));
        selectForms(dtoFormService, project, "ethics");

        stubFields(dtoFormService, project, dynamicField("ethics", "ethics_approval_status", "approved"));
        assertThat(templateIds(service, project)).containsExactly("ethics-report", "request");

        stubFields(dtoFormService, project, dynamicField("ethics", "ethics_approval_status", "pending"));
        assertThat(templateIds(service, project)).containsExactly("request");
    }

    @Test
    void aConditionCanRequireThatAFormIsSelected() {
        // Replaces the old form_titles selection: unselected forms are absent
        // from the condition's context, even if the backend returns their fields.
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata samples = metadata("samples");
        samples.setCondition("['samples'] != null");
        FormTemplateService service = service(dtoFormService, samples);
        stubFields(dtoFormService, project, dynamicField("samples", "volume", "5"));

        selectForms(dtoFormService, project, "project");
        assertThat(templateIds(service, project)).isEmpty();

        selectForms(dtoFormService, project, "project", "samples");
        assertThat(templateIds(service, project)).containsExactly("samples");
    }

    @Test
    void aConditionThatCannotBeEvaluatedIsNotMet() {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata broken = metadata("broken");
        broken.setCondition("['missing']['field']['value'] == 'x'");
        FormTemplateService service = service(dtoFormService, broken);
        selectForms(dtoFormService, project, "project");

        assertThat(templateIds(service, project)).isEmpty();
        assertThat(service.isTemplateAvailable(project, "broken", Optional.of("en"))).isFalse();
    }

    @Test
    void templatesWithoutConditionDoNotFetchTheProjectsFields() {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        FormTemplateService service = service(dtoFormService, metadata("request"));

        assertThat(templateIds(service, new Project())).containsExactly("request");
        verify(dtoFormService, never()).fetchProjectFormFields(any(), any(), any());
    }

    @Test
    void aTemplateExistsRegardlessOfItsCondition() {
        FormTemplateMetadata broken = metadata("broken");
        broken.setCondition("['missing']['field']['value'] == 'x'");
        FormTemplateService service = service(mock(DtoFormService.class), broken);

        assertThat(service.existsTemplate("broken")).isTrue();
        assertThat(service.existsTemplate("unknown")).isFalse();
    }

    @Test
    void anUnknownTemplateIsNotAvailable() {
        FormTemplateService service = service(mock(DtoFormService.class), metadata("request"));

        assertThat(service.isTemplateAvailable(new Project(), "unknown", Optional.of("en"))).isFalse();
        assertThat(service.isTemplateAvailable(new Project(), "request", Optional.of("en"))).isTrue();
    }

    @Test
    void printsTheSelectedFormsRestrictedToIncludeFormsWithoutExcludeForms() {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata();
        metadata.setIncludeForms(new String[]{"patient", "ethics", "funding"});
        metadata.setExcludeForms(new String[]{"ethics"});
        selectForms(dtoFormService, project, "patient", "ethics", "sample");
        when(dtoFormService.fetchProjectFormFields(Optional.empty(), project, Optional.of("en")))
                .thenReturn(List.of(
                        dynamicField("patient", "diagnosis"),
                        dynamicField("ethics", "ethics_approval_status"),
                        dynamicField("sample", "volume")));
        FormTemplateService service = formTemplateService(dtoFormService, metadata);

        Map<String, FormField> result = service.fetchFormFields(
                project, metadata.getTemplate(), "en", mock(ProjectContext.class));

        assertThat(result.values()).extracting(FormField::label).containsExactly("diagnosis");
    }

    @Test
    void theOnlyConfiguredTemplateIsTheDefaultImplicitly() {
        assertThat(service(mock(DtoFormService.class), metadata("request")).fetchDefaultTemplate())
                .contains("request");
    }

    @Test
    void theDefaultTemplateIsTheOneMarkedAsDefault() {
        FormTemplateMetadata request = metadata("request");
        request.setDefaultTemplate(true);

        assertThat(service(mock(DtoFormService.class), metadata("samples"), request).fetchDefaultTemplate())
                .contains("request");
        assertThat(service(mock(DtoFormService.class), metadata("samples"), metadata("other")).fetchDefaultTemplate())
                .isEmpty();
    }

    @Test
    void failsStartupWithMoreThanOneDefaultTemplate() {
        FormTemplateMetadata request = metadata("request");
        request.setDefaultTemplate(true);
        FormTemplateMetadata samples = metadata("samples");
        samples.setDefaultTemplate(true);

        assertThatThrownBy(() -> service(mock(DtoFormService.class), request, samples))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("request").hasMessageContaining("samples");
    }

    @Test
    void failsStartupWithAConditionThatIsNotAnExpression() {
        FormTemplateMetadata request = metadata("request");
        request.setCondition("['ethics'][");

        assertThatThrownBy(() -> service(mock(DtoFormService.class), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("request");
    }

    @Test
    void placesProjectFieldsByFormTitleThenLinkedFixedEntryThenHeader() {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata();
        metadata.setProjectFields(new FormTemplateFieldConfig[]{
                projectField("creator").build(),                          // no link: header
                projectField("PROJECT_TITLE").build(),                    // FIXED entry's form and order
                projectField("PROJECT_DESCRIPTION").build(),              // KEEP_FIXED_FIELD_ORDER: section start
                projectField("QUERIED_SITES").formTitle("query").build(), // form_title wins: section start
                projectField("ETHICS_VOTE").build()                       // FIXED entry inactive: hidden
        });
        selectForms(dtoFormService, project, "project", "query");
        when(dtoFormService.fetchProjectFormFields(Optional.empty(), project, Optional.of("en"))).thenReturn(List.of(
                fixedEntry("project", "PROJECT_TITLE", 2).build(),
                fixedEntry("project", "PROJECT_DESCRIPTION", 5).properties(new String[]{"KEEP_FIXED_FIELD_ORDER"}).build(),
                fixedEntry("project", "QUERIED_SITES", 6).build(),
                fixedEntry("project", "ETHICS_VOTE", 7).active(false).build(),
                dynamicField("project", "acronym", 1),
                dynamicField("project", "keywords", 3),
                dynamicField("query", "cohort", 1)));
        ProjectConfigurations order = new ProjectConfigurations();
        order.setFormTitleOrder(List.of("query", "project"));
        FormTemplateService service = projectFieldsService(dtoFormService, metadata, order);

        List<String> placed = service.fetchPlacedFields(project, metadata.getTemplate(), "en", new ProjectContext(Map.of()))
                .stream().map(field -> field.section() + " " + field.field().label()).toList();

        assertThat(placed).containsExactly(
                "null creator",
                "query QUERIED_SITES", "query cohort",
                "project PROJECT_DESCRIPTION", "project acronym", "project PROJECT_TITLE", "project keywords");
    }

    private FormTemplateMetadata metadata() {
        return metadata("project-overview");
    }

    private FormTemplateMetadata metadata(String template) {
        FormTemplateMetadata metadata = new FormTemplateMetadata();
        metadata.setTemplate(template);
        return metadata;
    }

    private void selectForms(DtoFormService dtoFormService, Project project, String... titles) {
        when(dtoFormService.fetchSelectedForms(project, Optional.of("en"))).thenReturn(
                Stream.of(titles).map(title -> new Form(title, null, null, null)).toList());
    }

    private FormField dynamicField(String title, String label) {
        return FormField.builder()
                .title(title).label(label).fieldType(FormFieldType.DYNAMIC).build();
    }

    private FormField dynamicField(String title, String label, String value) {
        return FormField.builder()
                .title(title).label(label).fieldType(FormFieldType.DYNAMIC).value(value).build();
    }

    private void stubFields(DtoFormService dtoFormService, Project project, FormField... fields) {
        when(dtoFormService.fetchProjectFormFields(Optional.empty(), project, Optional.of("en")))
                .thenReturn(List.of(fields));
    }

    private List<String> templateIds(FormTemplateService service, Project project) {
        return service.fetchTemplates(project, Optional.of("en")).stream()
                .map(FormTemplate::template).toList();
    }

    private static FormFieldConditionEvaluator conditionEvaluator() {
        return new FormFieldConditionEvaluator(mock(FormConfig.class));
    }

    /** A service whose configuration holds exactly these templates. */
    private FormTemplateService service(DtoFormService dtoFormService, FormTemplateMetadata... templates) {
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        Map<String, FormTemplateMetadata> byId = Arrays.stream(templates)
                .collect(Collectors.toMap(FormTemplateMetadata::getTemplate, t -> t));
        when(config.getTemplateMetadataMap()).thenReturn(byId);
        byId.forEach((id, template) -> when(config.getTemplate(id)).thenReturn(Optional.of(template)));
        DtoFactory dtoFactory = mock(DtoFactory.class);
        when(dtoFactory.convert(any(FormTemplateMetadata.class), any())).thenAnswer(invocation -> {
            FormTemplateMetadata template = invocation.getArgument(0);
            return new FormTemplate(template.getTemplate(), template.getTemplate());
        });
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        return new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config, dtoFactory,
                mock(DisplayFormatService.class), mock(ProjectContextFactory.class), new ProjectConfigurations(),
                mock(FormConfig.class), conditionEvaluator());
    }

    private static FormTemplateFieldConfig.FormTemplateFieldConfigBuilder<?, ?> projectField(String label) {
        return FormTemplateFieldConfig.builder().label(label).projectValue(label + "-value");
    }

    private static FormField.FormFieldBuilder fixedEntry(String title, String label, int order) {
        return FormField.builder().title(title).label(label).fieldType(FormFieldType.FIXED).order(order);
    }

    private FormField dynamicField(String title, String label, int order) {
        return FormField.builder()
                .title(title).label(label).fieldType(FormFieldType.DYNAMIC).order(order).build();
    }

    /** A service converting project fields into FormFields with their label. */
    private FormTemplateService projectFieldsService(
            DtoFormService dtoFormService, FormTemplateMetadata metadata, ProjectConfigurations order) {
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        when(config.getTemplate(metadata.getTemplate())).thenReturn(Optional.of(metadata));
        when(config.fetchProjectFormFieldTitle(metadata.getTemplate())).thenReturn("project-fields");
        DtoFactory dtoFactory = mock(DtoFactory.class);
        when(dtoFactory.convert(anyString(), any(FormFieldConfig.class), any(), any(), any(), any()))
                .thenAnswer(invocation -> FormField.builder()
                        .title(invocation.getArgument(0))
                        .label(((FormFieldConfig) invocation.getArgument(1)).getLabel())
                        .fieldType(FormFieldType.DYNAMIC)
                        .build());
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        return new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config, dtoFactory,
                mock(DisplayFormatService.class), mock(ProjectContextFactory.class), order,
                mock(FormConfig.class), conditionEvaluator());
    }

    private FormTemplateService formTemplateService(
            DtoFormService dtoFormService, FormTemplateMetadata metadata) {
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        when(config.getTemplate(metadata.getTemplate())).thenReturn(Optional.of(metadata));
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        return new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config,
                mock(DtoFactory.class), mock(DisplayFormatService.class), mock(ProjectContextFactory.class), new ProjectConfigurations(), mock(FormConfig.class), conditionEvaluator());
    }
}
