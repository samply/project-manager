package de.samply.form.template;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.Project;
import de.samply.display.DisplayFormatKey;
import de.samply.display.DisplayFormatService;
import de.samply.form.DataType;
import de.samply.form.DtoFormService;
import de.samply.form.FormConfig;
import de.samply.form.FormFieldConfig;
import de.samply.form.FormFieldLayout;
import de.samply.form.FormFieldLayoutRow;
import de.samply.form.FormFieldType;
import de.samply.form.condition.FormFieldConditionEvaluator;
import de.samply.form.pdf.FormPdfGeneratorFactory;
import de.samply.form.pdf.FormTemplateServiceException;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.configuration.ProjectConfigurations;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.frontend.dto.FormTemplate;
import de.samply.pdf.PdfGenerator;
import de.samply.pdf.PdfGeneratorException;
import de.samply.utils.FileExtension;
import de.samply.utils.FormFieldUtils;
import de.samply.utils.LanguageUtils;
import de.samply.form.template.FormTemplateFieldPlacement.PlacedField;
import de.samply.form.template.FormTemplateFieldPlacement.ProjectFieldPlacement;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class FormTemplateService {

    private static final String KEEP_FIXED_FIELD_ORDER = "KEEP_FIXED_FIELD_ORDER";

    private final DtoFormService dtoFormService;
    private final PdfGenerator pdfGenerator;
    private final String defaultLanguage;
    private final FormTemplateConfig formTemplateConfig;
    private final String defaultPdfFilename;
    private final DtoFactory dtoFactory;
    private final DisplayFormatService displayFormatService;
    private final ProjectContextFactory projectContextFactory;
    private final ProjectConfigurations frontendProjectConfigurations;
    private final FormConfig formConfig;
    private final FormFieldConditionEvaluator conditionEvaluator;


    public FormTemplateService(DtoFormService dtoFormService,
                               FormPdfGeneratorFactory pdfGeneratorFactory,
                               @Value(ProjectManagerConst.DEFAULT_LANGUAGE_SV) String defaultLanguage,
                               @Value(ProjectManagerConst.FORM_TEMPLATE_DEFAULT_PDF_FILENAME_SV) String defaultPdfFilename,
                               FormTemplateConfig formTemplateConfig,
                               DtoFactory dtoFactory,
                               DisplayFormatService displayFormatService,
                               ProjectContextFactory projectContextFactory,
                               ProjectConfigurations frontendProjectConfigurations,
                               FormConfig formConfig,
                               FormFieldConditionEvaluator conditionEvaluator) {
        this.dtoFormService = dtoFormService;
        this.pdfGenerator = pdfGeneratorFactory.createPdfGenerator();
        this.defaultLanguage = defaultLanguage;
        this.formTemplateConfig = formTemplateConfig;
        this.defaultPdfFilename = defaultPdfFilename;
        this.dtoFactory = dtoFactory;
        this.displayFormatService = displayFormatService;
        this.projectContextFactory = projectContextFactory;
        this.frontendProjectConfigurations = frontendProjectConfigurations;
        this.formConfig = formConfig;
        this.conditionEvaluator = conditionEvaluator;
        validateTemplateMetadata();
    }

    /**
     * Fails startup for configuration that can only be wrong (several default
     * templates, a condition that is not a valid expression, a section listed
     * twice) and warns about include/exclude form titles that match no
     * configured form and project fields placed in a section that exists
     * neither as a form nor in the template's forms.
     */
    private void validateTemplateMetadata() {
        Collection<FormTemplateMetadata> templates = formTemplateConfig.getTemplateMetadataMap().values();
        List<String> defaultTemplates = templates.stream()
                .filter(FormTemplateMetadata::isDefaultTemplate)
                .map(FormTemplateMetadata::getTemplate)
                .sorted()
                .toList();
        if (defaultTemplates.size() > 1) {
            throw new IllegalStateException("Only one form template may be the default: " + defaultTemplates);
        }
        Set<String> configuredFormTitles = formConfig.getFormTitleLabelFieldMap().keySet();
        for (FormTemplateMetadata template : templates) {
            if (template.getCondition() != null) {
                try {
                    conditionEvaluator.validateSyntax(template.getCondition());
                } catch (RuntimeException e) {
                    throw new IllegalStateException(
                            "Invalid condition in form template " + template.getTemplate(), e);
                }
            }
            Stream.of(template.getIncludeForms(), template.getExcludeForms())
                    .filter(Objects::nonNull)
                    .flatMap(Arrays::stream)
                    .filter(formTitle -> !configuredFormTitles.contains(formTitle))
                    .forEach(formTitle -> log.warn("Form template {} refers to unknown form {}",
                            template.getTemplate(), formTitle));
            List<String> sections = fetchTemplateForms(template).map(FormTemplateForm::getTitle).toList();
            if (sections.stream().distinct().count() < sections.size()) {
                throw new IllegalStateException(
                        "Form template " + template.getTemplate() + " lists a form title twice: " + sections);
            }
            Stream.ofNullable(template.getProjectFields())
                    .flatMap(Arrays::stream)
                    .map(FormTemplateFieldConfig::getFormTitle)
                    .filter(Objects::nonNull)
                    .filter(formTitle -> !configuredFormTitles.contains(formTitle) && !sections.contains(formTitle))
                    .forEach(formTitle -> log.warn(
                            "Form template {} places a project field in unknown section {} (it goes to the header)",
                            template.getTemplate(), formTitle));
        }
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
        // Add form fields, in print order
        List<PlacedField> placedFields = fetchPlacedFields(project, formTemplate, language, projectContext);
        Map<String, FormField> fields = toFieldsMap(placedFields);
        result.put(FormContextKey.FIELDS.getText(), fields);
        // Add form field layouts, so templates can group fields side by side
        // the same way the frontend does, instead of always one field per row.
        Map<String, List<FormFieldLayoutRow>> layouts = fetchFormLayouts(project, formTemplate, language);
        result.put(FormContextKey.LAYOUTS.getText(), layouts);
        // The document structure (headings and rows, layout rows resolved), so
        // the template only renders it - see FormTemplateDocumentBuilder.
        result.put(FormContextKey.DOCUMENT.getText(), FormTemplateDocumentBuilder.build(
                placedFields, fetchSections(resolveTemplateMetadata(formTemplate), project, language),
                FormFieldLayoutResolver.resolve(fields, layouts)));
        // Add form variables
        result.putAll(formTemplateConfig.fetchAllFormVariables(formTemplate, language));
        result.put(FormContextKey.DATA_TYPE_CLASS.getText(), DataType.class);
        result.put(FormContextKey.CURRENT_DATE.getText(), displayFormatService.format(
                DisplayFormatKey.LONG_DATE_FORMAT,
                Instant.now(),
                language,
                ZoneId.of(ProjectManagerConst.FORM_FILENAME_TIMESTAMP_ZONE)
        ));

        return result;
    }

    public Map<String, FormField> fetchFormFields(
            @NotNull Project project,
            @NotNull String formTemplate,
            @NotNull String language,
            @NotNull ProjectContext projectContext
    ) {
        return toFieldsMap(fetchPlacedFields(project, formTemplate, language, projectContext));
    }

    /**
     * Every field the PDF prints, with its section, in print order - see
     * {@link FormTemplateFieldPlacement} for the order and {@link #placeProjectField}
     * for where a project field goes.
     */
    public List<PlacedField> fetchPlacedFields(
            @NotNull Project project,
            @NotNull String formTemplate,
            @NotNull String language,
            @NotNull ProjectContext projectContext
    ) {
        FormTemplateMetadata template = resolveTemplateMetadata(formTemplate);
        Set<String> printedFormTitles = fetchPrintedFormTitles(template, project, language);
        // Fetched for all forms at once, like the frontend does, so conditions
        // are evaluated against every form's fields (a per-form fetch would
        // hide a field whose condition refers to another form).
        Collection<FormField> formFields =
                dtoFormService.fetchProjectFormFields(Optional.empty(), project, Optional.of(language));
        Map<String, FormField> fixedEntries = fetchFixedEntriesByLabel(formFields);

        List<ProjectFieldPlacement> projectFields = Stream.ofNullable(template.getProjectFields())
                .flatMap(Arrays::stream)
                // A project field configured "active": false is hidden from
                // the PDF the same way an inactive FIXED/DYNAMIC field is -
                // "hidden in the form field configuration" applies here too,
                // as opposed to a field only hidden by frontend-only role/UI
                // rules (e.g. an admin-only field), which should still print.
                .filter(FormFieldConfig::isActive)
                // Likewise when the FIXED entry it links to is inactive, or its
                // condition does not hold (the frontend hides the native field).
                .filter(config -> !isInactive(fixedEntries.get(config.getLabel())))
                .map(config -> placeProjectField(
                        formTemplate, config, fixedEntries.get(config.getLabel()), language, projectContext))
                .toList();

        // FIXED entries are metadata references for native frontend fields,
        // not form-template/PDF values.
        List<FormField> dynamicFields = formFields.stream()
                .filter(field -> field.fieldType() != FormFieldType.FIXED)
                .filter(field -> printedFormTitles.contains(field.title()))
                .toList();

        return FormTemplateFieldPlacement.place(fetchSectionOrder(template, printedFormTitles), projectFields, dynamicFields);
    }

    /**
     * A project field's section, in order of precedence: its own form_title;
     * else the form of the FIXED entry its label links to (where the frontend
     * shows the native field); else none - the header block. A section that is
     * not printed also means the header block (see FormTemplateFieldPlacement).
     * <p>
     * Following a FIXED entry, the field takes that entry's order among the
     * form's fields - unless the entry keeps the native order
     * (KEEP_FIXED_FIELD_ORDER), or form_title moved it to another form: then it
     * goes to the start of its section, where the frontend puts native fields.
     */
    private ProjectFieldPlacement placeProjectField(
            String formTemplate, FormTemplateFieldConfig config, FormField fixedEntry,
            String language, ProjectContext projectContext) {
        // A project field can leave its own display_name/description unset (or
        // explicitly blank) to defer to the linked FIXED field's own configured
        // metadata. Precedence: PDF template metadata (this field's own, if not
        // blank) > form-field metadata (the linked FIXED field's, if not blank)
        // > default (whatever's left, typically blank).
        FormTemplateFieldConfig resolved =
                projectContext.resolveProjectContext(applyFixedFieldMetadataFallback(config, language));
        FormField field = dtoFactory.convert(
                formTemplateConfig.fetchProjectFormFieldTitle(formTemplate),
                resolved,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(resolved.getProjectValue()),
                Optional.of(language));

        String fixedEntryForm = fixedEntry != null ? fixedEntry.title() : null;
        String section = config.getFormTitle() != null ? config.getFormTitle() : fixedEntryForm;
        boolean takesFixedEntryOrder = fixedEntry != null
                && section != null && section.equals(fixedEntryForm)
                && !hasProperty(fixedEntry, KEEP_FIXED_FIELD_ORDER);
        return new ProjectFieldPlacement(field, section, takesFixedEntryOrder ? fixedEntry.order() : null);
    }

    /**
     * FIXED entries by label, like the frontend: a label configured more than
     * once is ignored (the frontend warns and ignores it too).
     */
    private Map<String, FormField> fetchFixedEntriesByLabel(Collection<FormField> formFields) {
        Map<String, List<FormField>> byLabel = formFields.stream()
                .filter(field -> field.fieldType() == FormFieldType.FIXED)
                .collect(Collectors.groupingBy(FormField::label));
        Map<String, FormField> result = new HashMap<>();
        byLabel.forEach((label, entries) -> {
            if (entries.size() == 1) {
                result.put(label, entries.getFirst());
            } else {
                log.warn("Ignoring duplicate FIXED form-field key {}", label);
            }
        });
        return result;
    }

    private static boolean isInactive(FormField fixedEntry) {
        return fixedEntry != null && Boolean.FALSE.equals(fixedEntry.active());
    }

    private static boolean hasProperty(FormField field, String property) {
        return field.properties() != null && Arrays.asList(field.properties()).contains(property);
    }

    /**
     * The order of the sections: first those the template lists in its
     * forms, in that order - an existing form only if it is printed, a
     * template-only section always (it is printed only if a field is placed
     * in it); then the other printed forms in the deployment's canonical
     * section order (the frontend Summary's,
     * {@link ProjectConfigurations#getFormTitleOrder()}); a form missing from
     * both (config drift) comes last rather than being dropped.
     */
    private List<String> fetchSectionOrder(FormTemplateMetadata template, Set<String> printedFormTitles) {
        List<String> result = new ArrayList<>();
        fetchTemplateForms(template)
                .map(FormTemplateForm::getTitle)
                .filter(title -> printedFormTitles.contains(title) || !isConfiguredForm(title))
                .filter(title -> !result.contains(title))
                .forEach(result::add);
        frontendProjectConfigurations.getFormTitleOrder().stream()
                .filter(printedFormTitles::contains)
                .filter(title -> !result.contains(title))
                .forEach(result::add);
        printedFormTitles.stream()
                .filter(title -> !result.contains(title))
                .sorted()
                .forEach(result::add);
        return result;
    }

    /**
     * Heading metadata per section: the selected forms' own, overridden by
     * the template's forms entry where that sets a display name or
     * description, plus the template-only sections.
     */
    private Map<String, Form> fetchSections(FormTemplateMetadata template, Project project, String language) {
        Map<String, Form> result = dtoFormService.fetchSelectedForms(project, Optional.of(language)).stream()
                .collect(Collectors.toMap(Form::title, form -> form, (first, _) -> first, HashMap::new));
        fetchTemplateForms(template).forEach(section -> {
            Form form = result.getOrDefault(section.getTitle(), new Form(section.getTitle(), null, null, null));
            result.put(section.getTitle(), new Form(
                    form.title(),
                    override(section.getDisplayName(), language, form.titleDisplayName()),
                    override(section.getDescription(), language, form.titleDescription()),
                    override(section.getShortDescription(), language, form.titleShortDescription()),
                    form.titlePreInfo(),
                    form.titlePostInfo()));
        });
        return result;
    }

    private static Stream<FormTemplateForm> fetchTemplateForms(FormTemplateMetadata template) {
        return Stream.ofNullable(template.getForms())
                .flatMap(Arrays::stream)
                .filter(section -> section.getTitle() != null && !section.getTitle().isBlank());
    }

    private boolean isConfiguredForm(String title) {
        return formConfig.getFormTitleLabelFieldMap().containsKey(title);
    }

    // The template's text in this language (else the default language), if
    // set and not blank; otherwise the form's own.
    private String override(Map<String, String> templateValues, String language, String formValue) {
        if (templateValues == null) {
            return formValue;
        }
        String value = templateValues.getOrDefault(language, templateValues.get(defaultLanguage));
        return value != null && !value.isBlank() ? value : formValue;
    }

    private static Map<String, FormField> toFieldsMap(List<PlacedField> placedFields) {
        return placedFields.stream()
                .map(PlacedField::field)
                .collect(FormFieldUtils.formFieldMapCollector());
    }

    /**
     * Layout rows for every form this template prints, keyed by form title, in
     * the same scope as {@link #fetchFormFields}.
     * Flattened from {@link FormFieldLayout}'s own grouping (which has no
     * rendering significance of its own) down to a plain list of rows, since
     * a template only needs "which fields render together in one row" - not
     * how those rows happen to be grouped in configuration. Only form titles
     * that actually configure a layout are present in the result.
     */
    private Map<String, List<FormFieldLayoutRow>> fetchFormLayouts(
            @NotNull Project project, @NotNull String formTemplate, @NotNull String language) {
        Set<String> printedFormTitles = fetchPrintedFormTitles(
                resolveTemplateMetadata(formTemplate), project, language);

        return dtoFormService.fetchFormLayouts(Optional.empty()).entrySet().stream()
                .filter(entry -> printedFormTitles.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .flatMap(layout -> layout.rows().stream())
                                .toList(),
                        (existing, _) -> existing,
                        LinkedHashMap::new));
    }

    /**
     * The project's selected forms, restricted to the template's include_forms
     * (if set) and without its exclude_forms.
     */
    private Set<String> fetchPrintedFormTitles(FormTemplateMetadata template, Project project, String language) {
        Set<String> result = new HashSet<>(fetchSelectedFormTitles(project, Optional.of(language)));
        if (template.getIncludeForms() != null) {
            result.retainAll(Set.of(template.getIncludeForms()));
        }
        if (template.getExcludeForms() != null) {
            Arrays.asList(template.getExcludeForms()).forEach(result::remove);
        }
        return result;
    }

    private Set<String> fetchSelectedFormTitles(Project project, Optional<String> language) {
        return dtoFormService.fetchSelectedForms(project, language).stream()
                .map(Form::title)
                .collect(Collectors.toSet());
    }

    /**
     * Every configured template whose condition holds for this project (a
     * template without condition always does). Sorted by template id, so the
     * order is stable (templates are loaded into a HashMap).
     */
    public List<FormTemplate> fetchTemplates(@NotNull Project project, Optional<String> language) {
        List<FormTemplateMetadata> templates = formTemplateConfig.getTemplateMetadataMap().values().stream()
                .sorted(Comparator.comparing(FormTemplateMetadata::getTemplate))
                .toList();
        // Fetched once, and only if some template has a condition at all.
        Collection<FormField> conditionFields = templates.stream().anyMatch(t -> t.getCondition() != null)
                ? fetchConditionFields(project, language)
                : List.of();
        return templates.stream()
                .filter(template -> isConditionMet(template, conditionFields))
                .map(metadata -> dtoFactory.convert(metadata, language))
                .toList();
    }

    /**
     * Whether this template exists and its condition holds for the project -
     * the same rule {@link #fetchTemplates} applies, for the PDF download,
     * which receives the template id from the UI.
     */
    public boolean isTemplateAvailable(@NotNull Project project, String formTemplate, Optional<String> language) {
        return formTemplateConfig.getTemplate(formTemplate)
                .map(template -> template.getCondition() == null
                        || isConditionMet(template, fetchConditionFields(project, language)))
                .orElse(false);
    }

    /**
     * Whether this template is configured, regardless of its condition. For
     * callers that chose the template explicitly (email attachments), where
     * the condition, which only decides what the UI offers, does not apply.
     */
    public boolean existsTemplate(String formTemplate) {
        return formTemplateConfig.getTemplate(formTemplate).isPresent();
    }

    /**
     * The template marked "default", or the only configured template.
     */
    public Optional<String> fetchDefaultTemplate() {
        Collection<FormTemplateMetadata> templates = formTemplateConfig.getTemplateMetadataMap().values();
        if (templates.size() == 1) {
            return Optional.of(templates.iterator().next().getTemplate());
        }
        return templates.stream()
                .filter(FormTemplateMetadata::isDefaultTemplate)
                .map(FormTemplateMetadata::getTemplate)
                .findFirst();
    }

    private boolean isConditionMet(FormTemplateMetadata template, Collection<FormField> conditionFields) {
        return template.getCondition() == null
                || conditionEvaluator.isConditionMet(template.getCondition(), conditionFields);
    }

    /**
     * What a template condition sees: the dynamic fields of the project's
     * selected forms (all of them, not only the forms the template prints),
     * as the backend returns them - fields hidden by their own condition are
     * already left out.
     */
    private Collection<FormField> fetchConditionFields(Project project, Optional<String> language) {
        Set<String> selectedFormTitles = fetchSelectedFormTitles(project, language);
        return dtoFormService.fetchProjectFormFields(Optional.empty(), project, language).stream()
                .filter(field -> field.fieldType() != FormFieldType.FIXED)
                .filter(field -> selectedFormTitles.contains(field.title()))
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
     * 2026-09-07-plan-pdf-form-field-parity.md point 2 (2026-09-09 feedback).
     */
    private FormTemplateFieldConfig applyFixedFieldMetadataFallback(FormTemplateFieldConfig projectField, String language) {
        if (projectField.getLabel() == null) {
            return projectField;
        }
        return formConfig.fetchFixedFieldConfig(projectField.getLabel())
                .map(fixedConfig -> mergeBlankDisplayMetadata(projectField, fixedConfig, language))
                .orElse(projectField);
    }

    private FormTemplateFieldConfig mergeBlankDisplayMetadata(
            FormTemplateFieldConfig projectField, FormFieldConfig fixedConfig, String language) {
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
