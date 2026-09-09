package de.samply.form.template;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.form.DataType;
import de.samply.form.DtoFormService;
import de.samply.form.FormConfig;
import de.samply.form.FormFieldConfig;
import de.samply.form.FormFieldLayout;
import de.samply.form.FormFieldLayoutRow;
import de.samply.form.FormFieldType;
import de.samply.form.pdf.FormPdfGeneratorFactory;
import de.samply.form.pdf.FormTemplateServiceException;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.frontend.dto.Form;
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

    private final DtoFormService dtoFormService;
    private final PdfGenerator pdfGenerator;
    private final String defaultLanguage;
    private final FormTemplateConfig formTemplateConfig;
    private final String defaultPdfFilename;
    private final DtoFactory dtoFactory;
    private final String datePattern;
    private final ProjectContextFactory projectContextFactory;
    private final ProjectConfigurations frontendProjectConfigurations;
    private final FormConfig formConfig;


    public FormTemplateService(DtoFormService dtoFormService,
                               FormPdfGeneratorFactory pdfGeneratorFactory,
                               @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage,
                               @Value(ProjectManagerConst.FORM_TEMPLATE_DEFAULT_PDF_FILENAME_SV) String defaultPdfFilename,
                               FormTemplateConfig formTemplateConfig,
                               DtoFactory dtoFactory,
                               @Value(ProjectManagerConst.FORM_TEMPLATE_DATE_PATTERN_SV) String datePattern,
                               ProjectContextFactory projectContextFactory,
                               ProjectConfigurations frontendProjectConfigurations,
                               FormConfig formConfig) {
        this.dtoFormService = dtoFormService;
        this.pdfGenerator = pdfGeneratorFactory.createPdfGenerator();
        this.defaultLanguage = defaultLanguage;
        this.formTemplateConfig = formTemplateConfig;
        this.defaultPdfFilename = defaultPdfFilename;
        this.dtoFactory = dtoFactory;
        this.datePattern = datePattern;
        this.projectContextFactory = projectContextFactory;
        this.frontendProjectConfigurations = frontendProjectConfigurations;
        this.formConfig = formConfig;
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
            String templateFile = formTemplateConfig
                    .fetchTemplateFile(formTemplate)
                    .orElseThrow(() -> new RuntimeException("Template not found: " + formTemplate));
            return pdfGenerator.generatePdf(
                    templateFile,
                    createContext(project, formTemplate, LanguageUtils.normalize(language.orElse(defaultLanguage))));
        } catch (PdfGeneratorException e) {
            throw new FormTemplateServiceException(e);
        }
    }

    private Map<String, Object> createContext(Project project, String formTemplate, String language) {
        ProjectContext projectContext = projectContextFactory.createProjectContext(project, language);
        //Add project context
        Map<String, Object> result = new HashMap<>(projectContext.fetchContext());
        // Add form fields
        Map<String, FormField> fields = fetchFormFields(project, formTemplate, language, projectContext);
        result.put(FormContextKey.FIELDS.getText(), fields);
        // Add form field layouts, so templates can group fields side by side
        // the same way the frontend does, instead of always one field per row.
        Map<String, List<FormFieldLayoutRow>> layouts = fetchFormLayouts(project, formTemplate, language);
        result.put(FormContextKey.LAYOUTS.getText(), layouts);
        // Pre-resolve which fields belong together in a layout row, so a
        // template only needs two simple lookups per field instead of having
        // to cross-reference fields/layouts itself (SpringEL's collection
        // selection cannot see outer loop variables, making that impractical
        // to do reliably inside the template - see FormFieldLayoutResolver).
        result.put(FormContextKey.LAYOUT_RESOLVER.getText(), FormFieldLayoutResolver.resolve(fields, layouts));
        // Add form variables
        result.putAll(formTemplateConfig.fetchAllFormVariables(formTemplate, language));
        result.put(FormContextKey.DATA_TYPE_CLASS.getText(), DataType.class);
        result.put(FormContextKey.CURRENT_DATE.getText(), DateUtils.fetchCurrentDate(datePattern, language));

        return result;
    }

    public Map<String, FormField> fetchFormFields(
            @NotNull Project project,
            @NotNull String formTemplate,
            @NotNull String language,
            @NotNull ProjectContext projectContext
    ) {
        FormTemplateMetadata template = resolveTemplateMetadata(formTemplate);

        return Stream.concat(
                        // 1️⃣ ProjectCode fields
                        Stream.ofNullable(template.getProjectFields())
                                .flatMap(Arrays::stream)
                                // A project field configured "active": false is hidden from
                                // the PDF the same way an inactive FIXED/DYNAMIC field is -
                                // "hidden in the form field configuration" applies here too,
                                // as opposed to a field only hidden by frontend-only role/UI
                                // rules (e.g. an admin-only field), which should still print.
                                .filter(FormFieldConfig::isActive)
                                // A project field can leave its own display_name/
                                // description unset (or explicitly blank) to defer to
                                // the corresponding FIXED field's own configured
                                // metadata instead - point 2, 2026-09-09 feedback.
                                // Precedence: PDF template metadata (this field's own,
                                // if not blank) > form-field metadata (the linked FIXED
                                // field's, if not blank) > default (whatever's left,
                                // typically blank).
                                .map(field -> applyFixedFieldMetadataFallback(field, language))
                                .map(projectContext::resolveProjectContext)
                                .map(field -> dtoFactory.convert(
                                        formTemplateConfig.fetchProjectFormFieldTitle(formTemplate),
                                        field,
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.ofNullable(field.getProjectValue()),
                                        Optional.of(language)
                                )),

                        // 2️⃣ Form fields from formService (raw, base + override)
                        fetchApplicableFormTitles(template, project, language)
                                .flatMap(formTitle -> dtoFormService.fetchProjectFormFields(
                                        Optional.of(formTitle), project, Optional.of(language)).stream())
                )
                // FIXED entries are metadata references for native frontend
                // fields, not form-template/PDF values.
                .filter(field -> field.fieldType() != FormFieldType.FIXED)
                // Group by the deployment's canonical section order (point 1,
                // 2026-09-09 feedback), not FormFieldUtils.FORM_FIELD_COMPARATOR's
                // alphabetical-by-title-string order. Each per-title fetch above
                // is already correctly sorted internally (block instance/order/
                // label) by DtoFormService; Stream.sorted is stable, so comparing
                // by title index alone regroups titles into the right sequence
                // without disturbing that existing within-title order.
                .sorted(fetchPdfFieldOrderComparator())
                .collect(FormFieldUtils.formFieldMapCollector());
    }

    /**
     * Orders PDF fields by the same canonical section order the frontend
     * Summary uses ({@link ProjectConfigurations#getFormTitleOrder()}), so a
     * generated PDF's section sequence matches what the user already sees
     * there instead of an arbitrary alphabetical-by-title-string order.
     * Project fields (the synthetic "identity" section: name, email, request
     * ID, etc.) always come first, matching their current placement and
     * their role as a cover-page-style summary.
     */
    private Comparator<FormField> fetchPdfFieldOrderComparator() {
        List<String> canonicalOrder = frontendProjectConfigurations.getFormTitleOrder();
        Map<String, Integer> titleIndex = new HashMap<>();
        for (int i = 0; i < canonicalOrder.size(); i++) {
            titleIndex.putIfAbsent(canonicalOrder.get(i), i);
        }
        return Comparator.comparingInt(field -> fetchTitleOrderIndex(field, titleIndex));
    }

    private int fetchTitleOrderIndex(FormField field, Map<String, Integer> titleIndex) {
        if (formTemplateConfig.isProjectFormFieldTitle(field.title())) {
            return -1;
        }
        // A title missing from the canonical order (config drift) sorts last
        // rather than being dropped or crashing.
        return titleIndex.getOrDefault(field.title(), Integer.MAX_VALUE);
    }

    /**
     * Layout rows for every form title applicable to this project/template,
     * keyed by form title, in the same scope as {@link #fetchFormFields}.
     * Flattened from {@link FormFieldLayout}'s own grouping (which has no
     * rendering significance of its own) down to a plain list of rows, since
     * a template only needs "which fields render together in one row" - not
     * how those rows happen to be grouped in configuration. Only form titles
     * that actually configure a layout are present in the result.
     */
    private Map<String, List<FormFieldLayoutRow>> fetchFormLayouts(
            @NotNull Project project, @NotNull String formTemplate, @NotNull String language) {
        FormTemplateMetadata template = resolveTemplateMetadata(formTemplate);

        return fetchApplicableFormTitles(template, project, language)
                .map(title -> dtoFormService.fetchFormLayouts(Optional.of(title)))
                .flatMap(layoutsByTitle -> layoutsByTitle.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .flatMap(layout -> layout.rows().stream())
                                .toList(),
                        (existing, _) -> existing,
                        LinkedHashMap::new));
    }

    private Stream<String> fetchApplicableFormTitles(
            FormTemplateMetadata template, Project project, String language) {
        Stream<String> configuredFormTitles = Arrays.stream(template.getFormTitles());
        if (template.isAllFormTitlesRequired()) {
            return configuredFormTitles;
        }

        Set<String> selectedFormTitles = dtoFormService
                .fetchSelectedForms(project, Optional.of(language)).stream()
                .map(Form::title)
                .collect(Collectors.toSet());
        return configuredFormTitles.filter(selectedFormTitles::contains);
    }

    private List<FormTemplateMetadata> fetchValidMetadata(Project project, Optional<String> language) {

        // Fetch all selected forms for the project and extract their titles into a Set
        //  is used for O(1) lookup when checking if a title is contained
        Set<String> selectedFormTitles = dtoFormService.fetchSelectedForms(project, language).stream()
                .map(Form::title)
                .collect(Collectors.toSet());

        return formTemplateConfig.getTemplateMetadataMap().values().stream()
                .filter(metadata -> matchesSelectedForms(metadata, selectedFormTitles))
                .toList();
    }

    private boolean matchesSelectedForms(
            FormTemplateMetadata metadata, Set<String> selectedFormTitles) {
        Stream<String> formTitles = Arrays.stream(metadata.getFormTitles());
        return metadata.isAllFormTitlesRequired()
                ? formTitles.allMatch(selectedFormTitles::contains)
                : formTitles.anyMatch(selectedFormTitles::contains);
    }

    public List<FormTemplate> fetchTemplates(@NotNull Project project, Optional<String> language) {

        // Convert all valid metadata objects into DTOs (FormTemplate)
        // This method returns ALL matching templates without ranking
        return fetchValidMetadata(project, language).stream()
                .map(metadata -> dtoFactory.convert(metadata, language))
                .toList();
    }

    public List<FormTemplate> fetchBestTemplates(@NotNull Project project, Optional<String> language) {

        // Step 1: Get all templates that match their configured form-title requirement
        List<FormTemplateMetadata> validTemplates = fetchValidMetadata(project, language);

        // Step 2: Compute the "score" of each template
        // Here, the score is the number of form titles configured for the template
        int maxScore = validTemplates.stream()
                .mapToInt(metadata -> metadata.getFormTitles().length)
                .max()
                .orElse(0); // fallback if list is empty

        // Step 3: Keep only templates that have the maximum score
        // → i.e., the most specific / most complete templates
        return validTemplates.stream()
                .filter(metadata -> metadata.getFormTitles().length == maxScore)
                // Step 4: Convert the remaining metadata into DTOs for output
                .map(metadata -> dtoFactory.convert(metadata, language))
                .toList();
    }

    private FormTemplateMetadata resolveTemplateMetadata(String formTemplate) {
        return formTemplateConfig.getTemplate(formTemplate)
                .orElseThrow(() ->
                        new IllegalArgumentException("Template not found: " + formTemplate));
    }

    /**
     * A project field's "label" (optional, unrelated to the placeholders in
     * its own "value") can name a FIXED field's native key (e.g.
     * "ETHICS_VOTE_FOR_ALL_SITES") to link it to that field's own configured
     * display_name/description. When this project field's own display_name/
     * description is blank, the linked FIXED field's is used instead - see
     * plan-pdf-form-field-parity-2026-09-07.md point 2 (2026-09-09 feedback).
     */
    private FormFieldConfig applyFixedFieldMetadataFallback(FormFieldConfig projectField, String language) {
        if (projectField.getLabel() == null) {
            return projectField;
        }
        return formConfig.fetchFixedFieldConfig(projectField.getLabel())
                .map(fixedConfig -> mergeBlankDisplayMetadata(projectField, fixedConfig, language))
                .orElse(projectField);
    }

    private FormFieldConfig mergeBlankDisplayMetadata(
            FormFieldConfig projectField, FormFieldConfig fixedConfig, String language) {
        boolean needsDisplayName = isBlankFor(projectField.getDisplayName(), language)
                && !isBlankFor(fixedConfig.getDisplayName(), language);
        boolean needsDescription = isBlankFor(projectField.getDescription(), language)
                && !isBlankFor(fixedConfig.getDescription(), language);
        if (!needsDisplayName && !needsDescription) {
            return projectField;
        }
        return projectField.toBuilder()
                .displayName(needsDisplayName ? fixedConfig.getDisplayName() : projectField.getDisplayName())
                .description(needsDescription ? fixedConfig.getDescription() : projectField.getDescription())
                .build();
    }

    private boolean isBlankFor(Map<String, String> displayMap, String language) {
        if (displayMap == null) {
            return true;
        }
        String value = displayMap.getOrDefault(language, displayMap.get(defaultLanguage));
        return value == null || value.isBlank();
    }


}
