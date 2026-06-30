package de.samply.email.attachment;

import de.samply.db.model.Project;
import de.samply.form.template.FormTemplateService;
import de.samply.frontend.dto.FormTemplate;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

        return extra
                .filter(FormExtra.class::isInstance)
                .map(FormExtra.class::cast)
                .map(FormExtra::formTemplate)
                .or(() -> formTemplateService
                        .fetchTemplates(project, language)
                        .stream()
                        .findFirst()
                        .map(FormTemplate::template))
                .map(formTemplate -> new FilenameAndFileContent(
                        formTemplateService.fetchFormFilename(project, formTemplate),
                        formTemplateService.createFormAsPdf(project, formTemplate, language)
                ));
    }


}
