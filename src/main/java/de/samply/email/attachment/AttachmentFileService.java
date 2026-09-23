package de.samply.email.attachment;

import de.samply.db.model.Project;
import de.samply.form.template.FormTemplateService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class AttachmentFileService {

    private final FormTemplateService formTemplateService;

    public AttachmentFileService(FormTemplateService formTemplateService) {
        this.formTemplateService = formTemplateService;
    }

    public Optional<FilenameAndFileContent> fetchAttachmentFilenameAndContent(
            @NotNull Project project,
            @NotNull String attachmentFile,
            Optional<String> language) {
        return AttachmentFile
                .parse(attachmentFile)
                .flatMap(parsedAttachment -> switch (parsedAttachment.type()) {
                    case FORM -> fetchFormTemplateFilenameAndContent(project, parsedAttachment.extraInfo(), language);
                });
    }

    private Optional<FilenameAndFileContent> fetchFormTemplateFilenameAndContent(
            @NotNull Project project,
            Optional<AttachmentExtra> extra,
            Optional<String> language) {

        // "FORM:<template id>" names the template; plain "FORM" uses the default one.
        Optional<String> formTemplate = extra
                .filter(FormExtra.class::isInstance)
                .map(FormExtra.class::cast)
                .map(FormExtra::formTemplate)
                .or(formTemplateService::fetchDefaultTemplate);
        if (formTemplate.isEmpty()) {
            log.warn("Form attachment skipped for project {}: no default form template configured",
                    project.getCode());
            return Optional.empty();
        }
        // The template was chosen in the configuration, so its condition (which
        // only decides what the UI offers) does not apply here.
        if (!formTemplateService.existsTemplate(formTemplate.get())) {
            log.warn("Form attachment skipped for project {}: unknown form template {}",
                    project.getCode(), formTemplate.get());
            return Optional.empty();
        }
        return formTemplate.map(template -> new FilenameAndFileContent(
                formTemplateService.fetchFormFilename(project, template),
                formTemplateService.createFormAsPdf(project, template, language)
        ));
    }

}
