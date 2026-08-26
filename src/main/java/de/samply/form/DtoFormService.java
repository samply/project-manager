package de.samply.form;

import de.samply.db.model.Project;
import de.samply.form.condition.FormFieldConditionEvaluator;
import de.samply.frontend.dto.DtoFactory;
import de.samply.frontend.dto.Form;
import de.samply.frontend.dto.FormField;
import de.samply.project.DtoProjectService;
import de.samply.utils.FormFieldUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class DtoFormService {

    // Services
    private final FormService formService;
    private final DtoProjectService dtoProjectService;

    private final DtoFactory dtoFactory;
    private final FormConfig formConfig;
    private final FormFieldConditionEvaluator formFieldConditionEvaluator;


    public DtoFormService(
            FormService formService,
            DtoFactory dtoFactory,
            FormConfig formConfig,
            DtoProjectService dtoProjectService,
            FormFieldConditionEvaluator formFieldConditionEvaluator) {
        this.formService = formService;
        this.dtoFactory = dtoFactory;
        this.formConfig = formConfig;
        this.dtoProjectService = dtoProjectService;
        this.formFieldConditionEvaluator = formFieldConditionEvaluator;
    }

    public List<FormField> fetchProjectFormTitles(@NotNull Project project, Optional<String> language) {
        return formConfig
                .getFormTitleLabelFieldMap()
                .keySet()
                .stream()
                .map(title -> dtoFactory.convert(
                        title,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        language,
                        project.getState()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Resolves the configured title order to complete form metadata. Titles without
     * configured metadata are retained so the frontend can apply generic fallbacks.
     */
    public List<Form> fetchProjectFormTitleCanonicalOrder(
            Collection<String> formTitleOrder, @NotNull Project project, Optional<String> language) {
        return formTitleOrder.stream()
                .map(title -> dtoFactory.convertForm(title, language, project.getState()))
                .toList();
    }

    private List<FormField> fetchProjectFormFieldsDefinedInConfigWithoutValues(
            @NotNull String formTitle, Set<String> persistedLabels, @NotNull Project project,
            Optional<String> language
    ) {
        return formConfig
                .getFormTitleLabelFieldMap()
                .getOrDefault(formTitle, Map.of())
                .values()
                .stream()
                .filter(field -> field.getFieldType() == FormFieldType.DYNAMIC)
                // Keep inactive definitions only when this project already has
                // persisted data that still needs to be represented.
                .filter(field -> field.isActive() || persistedLabels.contains(field.getLabel()))
                .map(field ->
                        dtoFactory.convert(formTitle, field, Optional.empty(), Optional.empty(), Optional.empty(),
                                language, project.getState()))
                .toList();
    }

    private Stream<FormField> fetchFixedFormFieldMetadata(
            @NotNull String formTitle, @NotNull Project project, Optional<String> language) {
        // FIXED entries describe frontend-owned fields. They are returned once,
        // including when inactive, so the frontend can override/suppress its
        // native field. They deliberately bypass persisted values, conditions,
        // blocks and field-instance expansion.
        return formConfig
                .getFormTitleLabelFieldMap()
                .getOrDefault(formTitle, Map.of())
                .values()
                .stream()
                .filter(field -> field.getFieldType() == FormFieldType.FIXED)
                .map(field -> dtoFactory.convert(
                        formTitle, field, Optional.empty(), Optional.empty(), Optional.empty(),
                        language, project.getState()));
    }

    private List<FormField> fetchProjectFormFieldsWithValues(@NotNull String formTitle, @NotNull Project project, Optional<String> language) {
        return formService
                .fetchProjectFormFields(formTitle, project)
                .stream()
                .map(projectFormField -> dtoFactory.convert(projectFormField, language))
                .toList();
    }

    public List<Form> fetchSelectedForms(@NotNull Project project, Optional<String> language) {
        return Stream.concat(
                        // Fetch forms selected explicitly
                        formService.fetchSelectedForms(project)
                                .stream()
                                .map(projectForm -> dtoFactory.convertForm(
                                        projectForm.getFormTitle(), language, project.getState())),
                        // Fetch forms that should be selected according to the current configuration.
                        // This is particularly important if the current configuration is CUSTOM
                        dtoProjectService.fetchCurrentProjectConfigurations(project)
                                .stream()
                                .filter(projectAndForms -> projectAndForms.forms() != null)
                                .flatMap(projectAndForms -> Arrays.stream(projectAndForms.forms()))
                                .map(form -> dtoFactory.convertForm(form.title(), language, project.getState()))
                )
                // Remove duplicates
                .collect(Collectors.toMap(
                        Form::title,
                        Function.identity(),
                        (existing, _) -> existing
                ))
                .values()
                .stream()
                .toList();
    }

    private Stream<FormField> fetchBaseAndOverrideFormFields(
            @NotNull String formTitle, @NotNull Project project, Optional<String> language
    ) {
        // "valued" fields: the fields that actually have a value saved for this project,
        // each tagged with a concrete blockInstance.
        List<FormField> valuedFields = fetchProjectFormFieldsWithValues(formTitle, project, language);

        // Load persisted labels first, so filtering the base configuration does not
        // remove inactive fields that were used before they became inactive.
        Set<String> persistedLabels = valuedFields.stream()
                .map(FormField::label)
                .collect(Collectors.toSet());

        // Inactive fields are kept only for projects that already have data for them.
        List<FormField> baseFields = fetchProjectFormFieldsDefinedInConfigWithoutValues(
                formTitle, persistedLabels, project, language);

        return buildFormFieldsExpandingBlockInstances(baseFields, valuedFields);
    }

    /**
     * Combines base field definitions with valued fields, making sure that every
     * block instance that has AT LEAST ONE valued field ends up with ALL of its
     * fields present. (Using the valued one where it exists, and a "blank" clone
     * of the base definition - stamped with the instance number - where it doesn't.)
     * <p>
     * Fields that don't belong to a block are untouched: they're just concatenated
     * as before (base and valued), since the "missing instance" problem only applies
     * to blocks.
     */
    private Stream<FormField> buildFormFieldsExpandingBlockInstances(
            List<FormField> baseFields, List<FormField> valuedFields
    ) {
        // --- 1) Non-block fields: expand each one across its value instances. ---
        // (Every label with a value is guaranteed to also have a base
        // definition - see persistedLabels in fetchBaseAndOverrideFormFields -
        // so driving off the base map's labels doesn't miss any valued field.)
        Map<String, FormField> baseNonBlockByLabel = baseFields.stream()
                .filter(f -> f.block() == null)
                .collect(Collectors.toMap(FormField::label, Function.identity(), (a, _) -> a, LinkedHashMap::new));

        Map<String, List<FormField>> valuedNonBlockByLabel = valuedFields.stream()
                .filter(f -> f.block() == null)
                .collect(Collectors.groupingBy(FormField::label, LinkedHashMap::new, Collectors.toList()));

        Stream<FormField> nonBlockFields = baseNonBlockByLabel.entrySet().stream()
                .flatMap(entry -> expandFieldInstances(
                        entry.getValue(),
                        valuedNonBlockByLabel.getOrDefault(entry.getKey(), List.of())
                ));

        // --- 2) Group block fields by block name, preserving config order. ---
        // baseByBlock: block name -> ordered list of field definitions (F1, F2, F3, ...)
        Map<String, List<FormField>> baseByBlock = baseFields.stream()
                .filter(f -> f.block() != null)
                .collect(Collectors.groupingBy(FormField::block, LinkedHashMap::new, Collectors.toList()));

        // valuedByBlock: block name -> list of fields that have an actual value
        Map<String, List<FormField>> valuedByBlock = valuedFields.stream()
                .filter(f -> f.block() != null)
                .collect(Collectors.groupingBy(FormField::block, LinkedHashMap::new, Collectors.toList()));

        // Union of block names appearing in either map (a block could in theory
        // appear only in one of the two, though in practice it'll usually be both).
        Set<String> blockNames = new LinkedHashSet<>();
        blockNames.addAll(baseByBlock.keySet());
        blockNames.addAll(valuedByBlock.keySet());

        // --- 3) Expand each block independently, then flatten back into one stream. ---
        Stream<FormField> blockFields = blockNames.stream()
                .flatMap(block -> expandBlock(
                        baseByBlock.getOrDefault(block, List.of()),
                        valuedByBlock.getOrDefault(block, List.of())
                ));

        return Stream.concat(nonBlockFields, blockFields);
    }

    /**
     * Expands a single block's fields across all of its existing instances. If
     * no instance has been persisted yet, the configured minimum number of blank
     * instances is created for the response.
     * <p>
     * Example:
     * baseBlockFields = [F1, F2, F3] (config order, no instance, no value)
     * valuedBlockFields = [F1.1, F1.2] (only F1 has values, for instances 1 and 2)
     * <p>
     * Result (grouped by instance, base order preserved within each instance):
     * F1.1, F2.1 (blank), F3.1 (blank), F1.2, F2.2 (blank), F3.2 (blank)
     */
    private Stream<FormField> expandBlock(List<FormField> baseBlockFields, List<FormField> valuedBlockFields) {

        if (valuedBlockFields.isEmpty()) {
            // Block metadata is shared by all base fields in the same block, so
            // reading minBlockInstances from the first definition is enough.
            int minInstances = baseBlockFields.isEmpty()
                    ? 0
                    : Optional.ofNullable(baseBlockFields.getFirst().minBlockInstances()).orElse(0);

            if (minInstances > 0) {
                return IntStream.rangeClosed(1, minInstances)
                        .boxed()
                        .flatMap(instance -> baseBlockFields.stream()
                                .flatMap(base -> expandFieldInstances(withInstance(base, instance), List.of())));
            }

            return baseBlockFields.stream()
                    .flatMap(base -> expandFieldInstances(base, List.of()));
        }

        // Index valued fields as: label -> (block instance -> ITS value instances).
        // A plain (non-multiple) field always has exactly one value instance per
        // block instance, the same as before; a multiple field can have several -
        // see fieldInstance on ProjectFormField for why that's scoped per block
        // instance rather than globally per label.
        // NOTE: title is the *form* title (shared by every field), not the field's
        // own identity — label is what actually distinguishes F1 from F2 from F3.
        Map<String, Map<Integer, List<FormField>>> valuedByLabelAndInstance = valuedBlockFields.stream()
                .collect(Collectors.groupingBy(
                        FormField::label,
                        Collectors.groupingBy(FormField::blockInstance)
                ));

        Set<Integer> instances = valuedBlockFields.stream()
                .map(FormField::blockInstance)
                .collect(Collectors.toCollection(TreeSet::new));

        return instances.stream()
                .flatMap(instance -> baseBlockFields.stream()
                        .flatMap(base -> expandFieldInstances(
                                withInstance(base, instance),
                                valuedByLabelAndInstance
                                        .getOrDefault(base.label(), Map.of())
                                        .getOrDefault(instance, List.of())
                        ))
                );
    }

    /**
     * Expands a single field (already stamped with its block instance, if any)
     * across its own value instances - the field-level counterpart to
     * expandBlock above, one level deeper.
     * <p>
     * If the field's config has multiple = false, this preserves the exact
     * prior behavior: the one-valued instance if present, otherwise the blank
     * base. If multiple = true and at least one valued instance exists, every
     * valued instance is returned (sorted by fieldInstance), and the generic
     * blank base is dropped; if none exist yet, a single blank instance
     * stamped fieldInstance = 1 is returned, so there's always at least one
     * empty slot to render/fill in - the field-level equivalent of a block
     * with no persisted instances still needing something to show.
     */
    private Stream<FormField> expandFieldInstances(FormField base, List<FormField> valuedFieldInstances) {
        if (valuedFieldInstances.isEmpty()) {
            return Stream.of(Boolean.TRUE.equals(base.multiple()) ? withFirstFieldInstance(base) : base);
        }
        return valuedFieldInstances.stream()
                .sorted(Comparator.comparing(FormField::fieldInstance, Comparator.nullsLast(Integer::compareTo)));
    }

    /**
     * Clones a base field definition (no value, no instance) into a "blank"
     * field for a specific block instance: same everything, just blockInstance
     * set and value explicitly null.
     * <p>
     * Uses Lombok's generated toBuilder() (from @Builder(toBuilder = true) on
     * FormField) so this doesn't need to list all fields by hand and won't
     * silently go stale if FormField gains/loses a field later.
     */
    private FormField withInstance(FormField base, int instance) {
        return base.toBuilder()
                .blockInstance(instance)
                .value(null)
                .build();
    }

    // Unlike withInstance above, this has no "instance" parameter: there is no
    // minFieldInstances concept for multiple fields (only blocks have
    // minInstances) - a multiple field with no saved value yet always gets
    // exactly one blank slot to start from, fieldInstance = 1.
    private FormField withFirstFieldInstance(FormField base) {
        return base.toBuilder()
                .fieldInstance(1)
                .value(null)
                .build();
    }

    public Collection<FormField> fetchProjectFormFields(Optional<String> formTitle, @NotNull Project project, Optional<String> language) {
        List<String> formTitles = formTitle
                .map(List::of)
                .orElseGet(() -> formConfig.getFormTitleLabelFieldMap().keySet().stream().toList());

        Collection<FormField> dynamicFields = formFieldConditionEvaluator.filter(formTitles.stream()
                .flatMap(title -> fetchBaseAndOverrideFormFields(title, project, language))
                .sorted(FormFieldUtils.FORM_FIELD_COMPARATOR)
                .collect(FormFieldUtils.formFieldMapCollector())
                .values());

        return Stream.concat(
                        dynamicFields.stream(),
                        formTitles.stream().flatMap(title -> fetchFixedFormFieldMetadata(title, project, language)))
                .sorted(FormFieldUtils.FORM_FIELD_COMPARATOR)
                .collect(FormFieldUtils.formFieldMapCollector())
                .values();
    }

    public Map<String, List<FormFieldLayout>> fetchFormLayouts(Optional<String> formTitle) {
        return formConfig.getFormTitleLayoutsMap().entrySet().stream()
                .filter(entry -> formTitle.map(entry.getKey()::equals).orElse(true))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (_, layouts) -> layouts,
                        LinkedHashMap::new));
    }

}
