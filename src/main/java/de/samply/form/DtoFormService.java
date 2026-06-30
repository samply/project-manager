package de.samply.form;

import de.samply.db.model.Project;
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
import java.util.stream.Stream;

@Service
public class DtoFormService {

    // Services
    private final FormService formService;
    private final DtoProjectService dtoProjectService;

    private final DtoFactory dtoFactory;
    private final FormConfig formConfig;


    public DtoFormService(
            FormService formService,
            DtoFactory dtoFactory,
            FormConfig formConfig,
            DtoProjectService dtoProjectService) {
        this.formService = formService;
        this.dtoFactory = dtoFactory;
        this.formConfig = formConfig;
        this.dtoProjectService = dtoProjectService;
    }

    public List<FormField> fetchProjectFormTitles(Optional<String> language) {
        return formConfig
                .getFormTitleLabelFieldMap()
                .keySet()
                .stream()
                .map(title -> dtoFactory.convert(
                        title,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        language
                ))
                .collect(Collectors.toList());
    }

    public List<FormField> fetchProjectFormFieldsDefinedInConfigWithoutValues(@NotNull String formTitle, Optional<String> language) {
        return formConfig
                .getFormTitleLabelFieldMap()
                .getOrDefault(formTitle, Map.of())
                .values()
                .stream()
                .map(field ->
                        dtoFactory.convert(formTitle, field, Optional.empty(), Optional.empty(), language))
                .toList();
    }

    public List<FormField> fetchProjectFormFieldsWithValues(@NotNull String formTitle, @NotNull Project project, Optional<String> language) {
        return formService
                .fetchProjectFormFieldsWithValues(formTitle, project)
                .stream()
                .map(projectFormField -> dtoFactory.convert(projectFormField, language))
                .toList();
    }

    public List<Form> fetchSelectedForms(@NotNull Project project, Optional<String> language) {
        return Stream.concat(
                        // Fetch forms selected explicitly
                        formService.fetchSelectedForms(project)
                                .stream()
                                .map(projectForm -> dtoFactory.convert(projectForm, language)),
                        // Fetch forms that should be selected according to the current configuration.
                        // This is particularly important if the current configuration is CUSTOM
                        dtoProjectService.fetchCurrentProjectConfiguration(project)
                                .values()
                                .stream()
                                .flatMap(projectAndForms -> Arrays.stream(projectAndForms.forms()))
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

    public Stream<FormField> fetchBaseAndOverrideFormFields(
            @NotNull String formTitle, @NotNull Project project, Optional<String> language
    ) {
        // "base" fields: the plain field definitions from config, no value, no instance.
        List<FormField> baseFields = fetchProjectFormFieldsDefinedInConfigWithoutValues(formTitle, language);

        // "valued" fields: the fields that actually have a value saved for this project,
        // each tagged with a concrete blockInstance.
        List<FormField> valuedFields = fetchProjectFormFieldsWithValues(formTitle, project, language);

        return buildFormFieldsExpandingBlockInstances(baseFields, valuedFields);
    }

    /**
     * Combines base field definitions with valued fields, making sure that every
     * block instance that has AT LEAST ONE valued field ends up with ALL of its
     * fields present (using the valued one where it exists, and a "blank" clone
     * of the base definition - stamped with the instance number - where it doesn't).
     * <p>
     * Fields that don't belong to a block are untouched: they're just concatenated
     * as before (base and valued), since the "missing instance" problem only applies
     * to blocks.
     */
    private Stream<FormField> buildFormFieldsExpandingBlockInstances(
            List<FormField> baseFields, List<FormField> valuedFields
    ) {
        // --- 1) Non-block fields: keep the original, simple behavior. ---
        Stream<FormField> nonBlockFields = Stream.concat(
                baseFields.stream().filter(f -> f.block() == null),
                valuedFields.stream().filter(f -> f.block() == null)
        );

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
     * Expands a single block's fields across all of its existing instances.
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
            return baseBlockFields.stream();
        }

        // Index valued fields as: label -> (instance -> the valued FormField).
        // NOTE: title is the *form* title (shared by every field), not the field's
        // own identity — label is what actually distinguishes F1 from F2 from F3.
        Map<String, Map<Integer, FormField>> valuedByLabelAndInstance = valuedBlockFields.stream()
                .collect(Collectors.groupingBy(
                        FormField::label,
                        Collectors.toMap(FormField::blockInstance, Function.identity())
                ));

        Set<Integer> instances = valuedBlockFields.stream()
                .map(FormField::blockInstance)
                .collect(Collectors.toCollection(TreeSet::new));

        return instances.stream()
                .flatMap(instance -> baseBlockFields.stream()
                        .map(base -> valuedByLabelAndInstance
                                .getOrDefault(base.label(), Map.of())
                                .getOrDefault(instance, withInstance(base, instance))
                        )
                );
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

    public List<FormField> fetchProjectFormFields(Optional<String> formTitle, @NotNull Project project, Optional<String> language) {
        return formTitle
                .map(Stream::of)
                .orElseGet(() -> formConfig.getFormTitleLabelFieldMap().keySet().stream())
                .flatMap(title -> fetchBaseAndOverrideFormFields(title, project, language))
                .sorted(FormFieldUtils.FORM_FIELD_COMPARATOR)
                .collect(FormFieldUtils.formFieldMapCollector())
                .values()
                .stream()
                .toList();
    }


}
