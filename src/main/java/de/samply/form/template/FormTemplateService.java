package de.samply.form.template;

import de.samply.app.ProjectManagerConst;
import de.samply.form.DataType;
import de.samply.form.FormService;
import de.samply.form.pdf.FormPdfGeneratorFactory;
import de.samply.form.pdf.FormTemplateServiceException;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.FormTemplate;
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

    private final FormService formService;
    private final PdfGenerator pdfGenerator;
    private final String defaultFormTemplate;
    private final String defaultLanguage;
    private final FormTemplateConfig formTemplateConfig;
    private final String defaultPdfFilename;
    private final DtoFactory dtoFactory;
    private final String datePattern;
    private final ProjectContextFactory projectContextFactory;


    public FormTemplateService(FormService formService,
                               FormPdfGeneratorFactory pdfGeneratorFactory,
                               @Value(ProjectManagerConst.FORM_DEFAULT_TEMPLATE_SV) String defaultFormTemplate,
                               @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage,
                               @Value(ProjectManagerConst.FORM_TEMPLATE_DEFAULT_PDF_FILENAME_SV) String defaultPdfFilename,
                               FormTemplateConfig formTemplateConfig,
                               DtoFactory dtoFactory,
                               @Value(ProjectManagerConst.FORM_TEMPLATE_DATE_PATTERN_SV) String datePattern,
                               ProjectContextFactory projectContextFactory) {
        this.formService = formService;
        this.pdfGenerator = pdfGeneratorFactory.createPdfGenerator();
        this.defaultFormTemplate = defaultFormTemplate;
        this.defaultLanguage = defaultLanguage;
        this.formTemplateConfig = formTemplateConfig;
        this.defaultPdfFilename = defaultPdfFilename;
        this.dtoFactory = dtoFactory;
        this.datePattern = datePattern;
        this.projectContextFactory = projectContextFactory;
    }

    public String fetchFormFilename(@NotNull String projectCode, Optional<String> formTemplate) {

        return FormFilenameResolver.resolve(
                Optional.ofNullable(formTemplate.orElse(defaultFormTemplate))
                        .flatMap(formTemplateConfig::getTemplate)
                        .map(metadata -> metadata.getExtensionFilenameTemplateMap().get(FileExtension.PDF))
                        .orElse(defaultPdfFilename),
                Map.of(FormFilenameKey.PROJECT_CODE.getText(), projectCode)
        );
    }

    public byte[] createFormAsPdf(@NotNull String projectCode, Optional<String> formTemplate, Optional<String> language) throws FormTemplateServiceException {
        try {
            String template = formTemplate.orElse(defaultFormTemplate);
            return pdfGenerator.generatePdf(
                    template,
                    createContext(projectCode, template, LanguageUtils.normalize(language.orElse(defaultLanguage))));
        } catch (PdfGeneratorException e) {
            throw new FormTemplateServiceException(e);
        }
    }

    private Map<String, Object> createContext(String projectCode, String formTemplate, String language) {
        Map<String, Object> result = new HashMap<>();
        // Add form fields
        result.put(FormContextKey.FIELDS.getText(), fetchFormFields(projectCode, formTemplate, language));
        // Add form variables
        result.putAll(formTemplateConfig.fetchAllFormVariables(formTemplate, language));
        result.put(FormContextKey.DATA_TYPE_CLASS.getText(), DataType.class);
        result.put(FormContextKey.CURRENT_DATE.getText(), DateUtils.fetchCurrentDate(datePattern, language));

        return result;
    }

    private Map<String, FormField> fetchFormFields(
            @NotNull String projectCode,
            @NotNull String formTemplate,
            @NotNull String language
    ) {
        ProjectContext projectContext = projectContextFactory.createProjectContext(projectCode);

        // Resolve the template at once; fail fast if not found
        var template = formTemplateConfig.getTemplate(formTemplate)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + formTemplate));

        // 1️⃣ Project fields
        Stream<FormField> projectFields =
                Stream.ofNullable(template.getProjectFields())
                        .flatMap(Arrays::stream)
                        .map(projectContext::resolveProjectContext)
                        .map(field -> dtoFactory.convert(
                                formTemplateConfig.fetchProjectFormFieldTitle(formTemplate),
                                field,
                                Optional.ofNullable(field.getProjectValue()),
                                Optional.of(language)
                        ));

        // 2️⃣ Form fields for every title
        Stream<FormField> perTitleFields = Arrays.stream(template.getFormTitles())
                .flatMap(formTitle -> {
                    var baseFields = formService.fetchProjectFormFields(formTitle, Optional.of(language)).stream();
                    var overrideFields = formService.fetchProjectFormLabelAndValues(formTitle, projectCode, Optional.of(language)).stream();

                    // merge base + override
                    return Stream.concat(baseFields, overrideFields);
                });

        // 3️⃣ Merge projectFields with form fields
        return Stream.concat(projectFields, perTitleFields)
                .map(field -> Map.entry(fetchFormFieldKey(field), field))
                .sorted(Map.Entry.comparingByValue(FormFieldUtils.FORM_FIELD_COMPARATOR))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (_, newValue) -> newValue, // override duplicates
                        LinkedHashMap::new // preserve insertion order
                ));
    }


    private String fetchFormFieldKey(@NotNull FormField formField) {
        return formField.title() + formField.label();
    }

    public List<FormTemplate> fetchTemplates(Optional<String> language) {
        return formTemplateConfig.getTemplateMetadataMap().values().stream()
                .map(metadata -> dtoFactory.convert(metadata, language))
                .toList();
    }


}
