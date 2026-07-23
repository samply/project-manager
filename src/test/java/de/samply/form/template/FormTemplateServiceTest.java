package de.samply.form.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.db.model.Project;
import de.samply.form.DtoFormService;
import de.samply.form.pdf.FormPdfGeneratorFactory;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormTemplate;
import de.samply.pdf.PdfGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
                .map(title -> new Form(title, null, null))
                .toList());
        when(dtoFactory.convert(metadata, language)).thenReturn(template);

        FormTemplateService service = new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config, dtoFactory,
                "yyyy-MM-dd", mock(ProjectContextFactory.class));

        assertThat(service.fetchTemplates(project, language))
                .containsExactlyElementsOf(
                        expectedToMatch ? List.of(template) : List.<FormTemplate>of());
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
                new Form("patient", null, null),
                new Form("unrelated", null, null)));
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
        FormPdfGeneratorFactory pdfGeneratorFactory = mock(FormPdfGeneratorFactory.class);
        when(pdfGeneratorFactory.createPdfGenerator()).thenReturn(mock(PdfGenerator.class));
        FormTemplateConfig config = mock(FormTemplateConfig.class);
        when(config.getTemplate(metadata.getTemplate())).thenReturn(Optional.of(metadata));
        return new FormTemplateService(
                dtoFormService, pdfGeneratorFactory, "en", "form.pdf", config,
                mock(DtoFactory.class), "yyyy-MM-dd", mock(ProjectContextFactory.class));
    }
}
