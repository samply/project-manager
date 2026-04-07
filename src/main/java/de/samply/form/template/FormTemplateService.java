package de.samply.form.template;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.form.DataType;
import de.samply.form.pdf.FormPdfGeneratorFactory;
import de.samply.form.pdf.FormTemplateServiceException;
import de.samply.frontend.dto.*;
import de.samply.form.DtoFormService;
import de.samply.pdf.PdfGenerator;
import de.samply.pdf.PdfGeneratorException;
import de.samply.utils.DateUtils;
import de.samply.utils.FileExtension;
import de.samply.utils.FormFieldUtils;
import de.samply.utils.LanguageUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FormTemplateService {

    private final DtoFormService dtoFormService;
    private final PdfGenerator pdfGenerator;
    private final String defaultLanguage;
    private final FormTemplateConfig formTemplateConfig;
    private final String defaultPdfFilename;
    private final DtoFactory dtoFactory;
    private final String datePattern;
    private final ProjectContextFactory projectContextFactory;


    public FormTemplateService(DtoFormService dtoFormService,
                               FormPdfGeneratorFactory pdfGeneratorFactory,
                               @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage,
                               @Value(ProjectManagerConst.FORM_TEMPLATE_DEFAULT_PDF_FILENAME_SV) String defaultPdfFilename,
                               FormTemplateConfig formTemplateConfig,
                               DtoFactory dtoFactory,
                               @Value(ProjectManagerConst.FORM_TEMPLATE_DATE_PATTERN_SV) String datePattern,
                               ProjectContextFactory projectContextFactory) {
        this.dtoFormService = dtoFormService;
        this.pdfGenerator = pdfGeneratorFactory.createPdfGenerator();
        this.defaultLanguage = defaultLanguage;
        this.formTemplateConfig = formTemplateConfig;
        this.defaultPdfFilename = defaultPdfFilename;
        this.dtoFactory = dtoFactory;
        this.datePattern = datePattern;
        this.projectContextFactory = projectContextFactory;
    }

    public String fetchFormFilename(@NotNull Project project, String formTemplate) {
        return FormFilenameResolver.resolve(
                formTemplateConfig.getTemplate(formTemplate)
                        .map(metadata -> metadata.getExtensionFilenameTemplateMap().get(FileExtension.PDF))
                        .orElse(defaultPdfFilename),
                Map.of(FormFilenameKey.PROJECT_CODE.getText(), project.getCode())
        );
    }

    public byte[] createFormAsPdf(@NotNull Project project, @NotNull String formTemplate, Optional<String> language) throws FormTemplateServiceException {
        try {
            return pdfGenerator.generatePdf(
                    formTemplate,
                    createContext(project, formTemplate, LanguageUtils.normalize(language.orElse(defaultLanguage))));
        } catch (PdfGeneratorException e) {
            throw new FormTemplateServiceException(e);
        }
    }

    private Map<String, Object> createContext(Project project, String formTemplate, String language) {
        Map<String, Object> result = new HashMap<>();
        // Add form fields
        result.put(FormContextKey.FIELDS.getText(), fetchFormFields(project, formTemplate, language));
        // Add form variables
        result.putAll(formTemplateConfig.fetchAllFormVariables(formTemplate, language));
        result.put(FormContextKey.DATA_TYPE_CLASS.getText(), DataType.class);
        result.put(FormContextKey.CURRENT_DATE.getText(), DateUtils.fetchCurrentDate(datePattern, language));

        return result;
    }

    public Map<String, FormField> fetchFormFields(
            @NotNull Project project,
            @NotNull String formTemplate,
            @NotNull String language
    ) {
        FormTemplateMetadata template = resolveTemplateMetadata(formTemplate);
        ProjectContext projectContext = projectContextFactory.createProjectContext(project, language);

        return Stream.concat(
                        // 1️⃣ ProjectCode fields
                        Stream.ofNullable(template.getProjectFields())
                                .flatMap(Arrays::stream)
                                .map(projectContext::resolveProjectContext)
                                .map(field -> dtoFactory.convert(
                                        formTemplateConfig.fetchProjectFormFieldTitle(formTemplate),
                                        field,
                                        Optional.ofNullable(field.getProjectValue()),
                                        Optional.of(language)
                                )),

                        // 2️⃣ Form fields from formService (raw, base + override)
                        Arrays.stream(template.getFormTitles())
                                .flatMap(formTitle -> dtoFormService.fetchBaseAndOverrideFormFields(
                                        formTitle, project, Optional.of(language)))
                )
                .sorted(FormFieldUtils.FORM_FIELD_COMPARATOR)
                .collect(FormFieldUtils.formFieldMapCollector());
    }


    public List<FormTemplate> fetchTemplates(@NotNull Project project, Optional<String> language) {
        Set<String> selectedFormTitles = dtoFormService.fetchSelectedForms(project, language).stream()
                .map(Form::title)
                .collect(Collectors.toSet());

        return formTemplateConfig.getTemplateMetadataMap().values().stream()
                .filter(metadata -> Arrays.stream(metadata.getFormTitles())
                        .allMatch(selectedFormTitles::contains)
                )
                .map(metadata -> dtoFactory.convert(metadata, language))
                .toList();
    }

    private FormTemplateMetadata resolveTemplateMetadata(String formTemplate) {
        return formTemplateConfig.getTemplate(formTemplate)
                .orElseThrow(() ->
                        new IllegalArgumentException("Template not found: " + formTemplate));
    }


}
