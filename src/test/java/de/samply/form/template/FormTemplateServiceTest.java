package de.samply.form.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.db.model.Project;
import de.samply.form.DtoFormService;
import de.samply.form.FormConfig;
import de.samply.form.FormFieldConfig;
import de.samply.form.FormFieldLayout;
import de.samply.form.FormFieldLayoutRow;
import de.samply.form.FormFieldType;
import de.samply.form.pdf.FormPdfGeneratorFactory;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.FormTemplate;
import de.samply.pdf.PdfGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormTemplateServiceTest {

    @ParameterizedTest
    @MethodSource("formSelections")
    void selectsMetadataAccordingToItsRequiredFormMode(
            boolean allFormTitlesRequired, List<String> selectedTitles, boolean expectedToMatch) {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        DtoFactory dtoFactory = mock(DtoFactory.class);
        Project project = new Project();
        Optional<String> language = Optional.of("en");

        FormTemplateMetadata metadata = new FormTemplateMetadata();
        metadata.setTemplate("project-overview");
        metadata.setFormTitles(new String[]{"patient", "sample"});
        metadata.setAllFormTitlesRequired(allFormTitlesRequired);
        FormTemplate template = new FormTemplate("project-overview", "Project overview");

        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        when(config.getTemplateMetadataMap()).thenReturn(Map.of(metadata.getTemplate(), metadata));
        when(dtoFormService.fetchSelectedForms(project, language)).thenReturn(selectedTitles.stream()
                .map(title -> new Form(title, null, null, null))
                .toList());
        when(dtoFactory.convert(metadata, language)).thenReturn(template);

        FormTemplateService service = new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config, dtoFactory,
                "yyyy-MM-dd", mock(ProjectContextFactory.class), new ProjectConfigurations(), mock(FormConfig.class));

        assertThat(service.fetchTemplates(project, language))
                .containsExactlyElementsOf(
                        expectedToMatch ? List.of(template) : List.of());
    }

    @Test
    void defaultsToRequiringAnyFormTitleWhenPropertyIsOmitted() throws Exception {
        FormTemplateMetadata metadata = new ObjectMapper().readValue("""
                {
                  "template": "project-overview",
                  "form_titles": ["patient", "sample"]
                }
                """, FormTemplateMetadata.class);

        assertThat(metadata.isAllFormTitlesRequired()).isFalse();
    }

    @Test
    void fetchesEveryConfiguredFormForStrictMetadata() {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata(true);
        FormTemplateService service = formTemplateService(dtoFormService, metadata);

        service.fetchFormFields(
                project, metadata.getTemplate(), "en", mock(ProjectContext.class));

        verify(dtoFormService, never()).fetchSelectedForms(project, Optional.of("en"));
        verify(dtoFormService).fetchProjectFormFields(
                Optional.of("patient"), project, Optional.of("en"));
        verify(dtoFormService).fetchProjectFormFields(
                Optional.of("sample"), project, Optional.of("en"));
    }

    @Test
    void fetchesOnlySelectedConfiguredFormsForTolerantMetadata() {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata(false);
        when(dtoFormService.fetchSelectedForms(project, Optional.of("en"))).thenReturn(List.of(
                new Form("patient", null, null, null),
                new Form("unrelated", null, null, null)));
        FormTemplateService service = formTemplateService(dtoFormService, metadata);

        service.fetchFormFields(
                project, metadata.getTemplate(), "en", mock(ProjectContext.class));

        verify(dtoFormService).fetchProjectFormFields(
                Optional.of("patient"), project, Optional.of("en"));
        verify(dtoFormService, never()).fetchProjectFormFields(
                Optional.of("sample"), project, Optional.of("en"));
        verify(dtoFormService, never()).fetchProjectFormFields(
                Optional.of("unrelated"), project, Optional.of("en"));
    }

    @Test
    void excludesFixedFrontendMetadataFromTemplateFieldValues() {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata(true);
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
                Optional.of("patient"), project, Optional.of("en")))
                .thenReturn(List.of(fixed, dynamic));
        when(dtoFormService.fetchProjectFormFields(
                Optional.of("sample"), project, Optional.of("en")))
                .thenReturn(List.of());
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
        // rule - see plan-pdf-form-field-parity-2026-09-07.md point 3.
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata(true);
        metadata.setProjectFields(new FormFieldConfig[]{
                FormFieldConfig.builder().label("visible-field").active(true)
                        .projectValue("${project-code}").build(),
                FormFieldConfig.builder().label("hidden-field").active(false)
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
                dtoFactory, "yyyy-MM-dd", mock(ProjectContextFactory.class), new ProjectConfigurations(), mock(FormConfig.class));

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
        FormTemplateMetadata metadata = metadata(true);
        // Declared here in the "wrong" (alphabetical) order on purpose, to
        // prove the fix doesn't depend on form_titles happening to already
        // be listed in canonical order.
        metadata.setFormTitles(new String[]{"project", "query"});
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        when(config.getTemplate(metadata.getTemplate())).thenReturn(Optional.of(metadata));
        when(config.isProjectFormFieldTitle(anyString())).thenReturn(false);
        FormField projectField = FormField.builder()
                .title("project").label("methodology").fieldType(FormFieldType.DYNAMIC).build();
        FormField queryField = FormField.builder()
                .title("query").label("cohort").fieldType(FormFieldType.DYNAMIC).build();
        when(dtoFormService.fetchProjectFormFields(Optional.of("project"), project, Optional.of("en")))
                .thenReturn(List.of(projectField));
        when(dtoFormService.fetchProjectFormFields(Optional.of("query"), project, Optional.of("en")))
                .thenReturn(List.of(queryField));
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        ProjectConfigurations canonicalOrder = new ProjectConfigurations();
        canonicalOrder.setFormTitleOrder(List.of("query", "project"));
        FormTemplateService service = new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config,
                mock(DtoFactory.class), "yyyy-MM-dd", mock(ProjectContextFactory.class), canonicalOrder, mock(FormConfig.class));

        Map<String, FormField> result = service.fetchFormFields(
                project, metadata.getTemplate(), "en", new ProjectContext(Map.of()));

        assertThat(result.values()).extracting(FormField::title).containsExactly("query", "project");
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
        FormTemplateMetadata metadata = metadata(true);
        metadata.setProjectFields(new FormFieldConfig[]{
                // Blank display_name entirely omitted; description explicitly
                // blank - both must fall back to the linked FIXED field's.
                FormFieldConfig.builder().label("ETHICS_VOTE_FOR_ALL_SITES")
                        .description(Map.of("en", ""))
                        .projectValue("${ethics-vote-for-all-sites-filename}").build(),
                // Has its own non-blank display_name - must NOT be overridden
                // by the (also configured) FIXED field's.
                FormFieldConfig.builder().label("QUERY_FORMAT")
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
                dtoFactory, "yyyy-MM-dd", mock(ProjectContextFactory.class), new ProjectConfigurations(), formConfig);

        service.fetchFormFields(project, metadata.getTemplate(), "en", new ProjectContext(Map.of()));

        Map<String, FormFieldConfig> byLabel = convertedConfig.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(FormFieldConfig::getLabel, c -> c));
        assertThat(byLabel.get("ETHICS_VOTE_FOR_ALL_SITES").getDescription())
                .containsEntry("en", "Upload ethics vote file (optional)");
        assertThat(byLabel.get("QUERY_FORMAT").getDisplayName())
                .containsEntry("en", "Query format (PDF override)");
    }

    @Test
    void mergesFormFieldLayoutsFromEveryApplicableFormTitleIntoThePdfContext() throws Exception {
        DtoFormService dtoFormService = mock(DtoFormService.class);
        Project project = new Project();
        FormTemplateMetadata metadata = metadata(true);
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        when(config.getTemplate(metadata.getTemplate())).thenReturn(Optional.of(metadata));
        when(config.fetchTemplateFile(metadata.getTemplate())).thenReturn(Optional.of("request"));
        when(config.fetchAllFormVariables(metadata.getTemplate(), "en")).thenReturn(Map.of());
        ProjectContextFactory projectContextFactory = mock(ProjectContextFactory.class);
        when(projectContextFactory.createProjectContext(project, "en"))
                .thenReturn(new ProjectContext(Map.of()));
        FormFieldLayoutRow patientRow = new FormFieldLayoutRow(List.of("first_name", "last_name"));
        FormFieldLayoutRow sampleRow = new FormFieldLayoutRow(List.of("volume", "unit"));
        when(dtoFormService.fetchFormLayouts(Optional.of("patient")))
                .thenReturn(Map.of("patient", List.of(new FormFieldLayout(List.of(patientRow)))));
        when(dtoFormService.fetchFormLayouts(Optional.of("sample")))
                .thenReturn(Map.of("sample", List.of(new FormFieldLayout(List.of(sampleRow)))));
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        PdfGenerator pdfGenerator = mock(PdfGenerator.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(pdfGenerator);
        FormTemplateService service = new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config,
                mock(DtoFactory.class), "yyyy-MM-dd", projectContextFactory, new ProjectConfigurations(), mock(FormConfig.class));

        service.createFormAsPdf(project, metadata.getTemplate(), Optional.of("en"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> contextCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(pdfGenerator).generatePdf(anyString(), contextCaptor.capture());
        assertThat(contextCaptor.getValue())
                .containsEntry(FormContextKey.LAYOUTS.getText(), Map.of(
                        "patient", List.of(patientRow),
                        "sample", List.of(sampleRow)));
    }

    private static Stream<Arguments> formSelections() {
        return Stream.of(
                Arguments.of(false, List.of("patient"), true),
                Arguments.of(false, List.of("other"), false),
                Arguments.of(true, List.of("patient"), false),
                Arguments.of(true, List.of("patient", "sample"), true)
        );
    }

    private FormTemplateMetadata metadata(boolean allFormTitlesRequired) {
        FormTemplateMetadata metadata = new FormTemplateMetadata();
        metadata.setTemplate("project-overview");
        metadata.setFormTitles(new String[]{"patient", "sample"});
        metadata.setAllFormTitlesRequired(allFormTitlesRequired);
        return metadata;
    }

    private FormTemplateService formTemplateService(
            DtoFormService dtoFormService, FormTemplateMetadata metadata) {
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        when(config.getTemplate(metadata.getTemplate())).thenReturn(Optional.of(metadata));
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        return new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config,
                mock(DtoFactory.class), "yyyy-MM-dd", mock(ProjectContextFactory.class), new ProjectConfigurations(), mock(FormConfig.class));
    }
}
