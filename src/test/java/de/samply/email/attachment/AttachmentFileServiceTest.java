package de.samply.email.attachment;

import de.samply.db.model.Project;
import de.samply.form.template.FormTemplateService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachmentFileServiceTest {

    private static final Optional<String> LANGUAGE = Optional.of("en");

    private final FormTemplateService formTemplateService = mock(FormTemplateService.class);
    private final AttachmentFileService service = new AttachmentFileService(formTemplateService);
    private final Project project = new Project();

    @Test
    void parsesAPlainFormAndAFormWithTemplateId() {
        assertThat(AttachmentFile.parse("FORM"))
                .hasValueSatisfying(parsed -> assertThat(parsed.extraInfo()).isEmpty());
        // Template ids may contain "-": only the first ":" separates.
        assertThat(AttachmentFile.parse("FORM:clinical-data"))
                .hasValueSatisfying(parsed -> assertThat(parsed.extraInfo())
                        .contains(new FormExtra("clinical-data")));
    }

    @Test
    void aPlainFormAttachesTheDefaultTemplate() {
        when(formTemplateService.fetchDefaultTemplate()).thenReturn(Optional.of("request"));
        makeAvailable("request");

        assertThat(service.fetchAttachmentFilenameAndContent(project, "FORM", LANGUAGE))
                .hasValueSatisfying(file -> assertThat(file.filename()).isEqualTo("request.pdf"));
    }

    @Test
    void aFormWithTemplateIdAttachesThatTemplate() {
        when(formTemplateService.fetchDefaultTemplate()).thenReturn(Optional.of("request"));
        makeAvailable("samples");

        assertThat(service.fetchAttachmentFilenameAndContent(project, "FORM:samples", LANGUAGE))
                .hasValueSatisfying(file -> assertThat(file.filename()).isEqualTo("samples.pdf"));
    }

    @Test
    void attachesAConfiguredTemplateEvenIfItsConditionDoesNotHold() {
        // The condition only decides what the UI offers; a template chosen in
        // email-templates.json is always generated.
        makeAvailable("samples");
        when(formTemplateService.isTemplateAvailable(project, "samples", LANGUAGE)).thenReturn(false);

        assertThat(service.fetchAttachmentFilenameAndContent(project, "FORM:samples", LANGUAGE))
                .hasValueSatisfying(file -> assertThat(file.filename()).isEqualTo("samples.pdf"));
        verify(formTemplateService, never()).isTemplateAvailable(any(), anyString(), any());
    }

    @Test
    void skipsTheAttachmentForAnUnknownTemplate() {
        when(formTemplateService.existsTemplate("samples")).thenReturn(false);

        assertThat(service.fetchAttachmentFilenameAndContent(project, "FORM:samples", LANGUAGE)).isEmpty();
        verify(formTemplateService, never()).createFormAsPdf(any(), anyString(), any());
    }

    @Test
    void skipsAPlainFormAttachmentWithoutDefaultTemplate() {
        when(formTemplateService.fetchDefaultTemplate()).thenReturn(Optional.empty());

        assertThat(service.fetchAttachmentFilenameAndContent(project, "FORM", LANGUAGE)).isEmpty();
        verify(formTemplateService, never()).createFormAsPdf(any(), anyString(), any());
    }

    private void makeAvailable(String template) {
        when(formTemplateService.existsTemplate(template)).thenReturn(true);
        when(formTemplateService.fetchFormFilename(project, template)).thenReturn(template + ".pdf");
        when(formTemplateService.createFormAsPdf(project, template, LANGUAGE)).thenReturn(new byte[]{1});
    }
}
