package de.samply.form.core;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.app.ProjectManagerConst;
import de.samply.form.core.model.ContextualDisplayMetadata;
import de.samply.form.core.model.DataType;
import de.samply.form.core.model.DisplayInfo;
import de.samply.form.core.model.DisplayMetadata;
import de.samply.form.core.model.FormFieldBlock;
import de.samply.form.core.model.FormFieldConfig;
import de.samply.form.core.model.FormFieldLayout;
import de.samply.form.core.model.FormFieldType;
import de.samply.form.core.model.FormMetadataConfig;
import de.samply.utils.directory.ExistingDirectory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Configuration
@Getter
public class FormConfig {

    // Blocks and groups are defined per form: keyed by form title, then label.
    private final Map<String, Map<String, FormFieldBlock>> formTitleBlockMap = new HashMap<>();
    private final Map<String, ContextualDisplayMetadata> formTitleDisplaMetadataMap = new HashMap<>();
    private final Map<String, Map<String, DisplayMetadata>> formTitleGroupMap = new HashMap<>();
    // A label can be configured several times in a form ("instances", tried in order by their conditions, see
    // FormFieldConfig.condition). This map holds the first instance, for callers without project values.
    private final Map<String, Map<String, FormFieldConfig>> formTitleLabelFieldMap = new HashMap<>();
    // Every instance of a label, in configuration order.
    private final Map<String, Map<String, List<FormFieldConfig>>> formTitleLabelInstancesMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> formTitleLabelOrderMap = new HashMap<>();
    private final Map<String, List<FormFieldLayout>> formTitleLayoutsMap = new HashMap<>();
    // Each form's configuration file as JSON (one file per form title) - the
    // form's definition as configured.
    private final Map<String, JsonNode> formTitleJsonMap = new LinkedHashMap<>();
    private final Set<String> inactiveFormTitles = new HashSet<>();
    private final Map<String, Path> formTitleFileMap = new HashMap<>();
    private final Map<String, String> fixedLabelFormTitleMap = new HashMap<>();
    // Identifiers configured twice, found while loading (see validateUniqueness).
    private final List<String> duplicates = new ArrayList<>();
    // Instances of a field label that do not fit together, found while loading (see validateInstances).
    private final List<String> invalidInstances = new ArrayList<>();

    public FormConfig(@Value(ProjectManagerConst.FORM_FIELDS_DIRECTORY_SV) ExistingDirectory configDir
    ) {
        // A key twice in one JSON object (e.g. a group id, a language) is an
        // error with file and line, instead of the last one silently winning.
        ObjectMapper objectMapper = new ObjectMapper(
                JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());

        try (Stream<Path> files = Files.list(configDir.path())) {
            files
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted() // deterministic order
                    .forEach(path -> loadConfigFile(path, objectMapper));
        } catch (IOException e) {
            log.error("Failed to read form config directory {}", configDir, e);
        }

        List<String> brokenReferences = findBrokenReferences();
        if (!duplicates.isEmpty() || !invalidInstances.isEmpty() || !brokenReferences.isEmpty()) {
            duplicates.forEach(duplicate -> log.error("Invalid form configuration: configured twice: {}", duplicate));
            invalidInstances.forEach(instance -> log.error("Invalid form configuration: {}", instance));
            brokenReferences.forEach(reference -> log.error("Invalid form configuration: {}", reference));
            List<String> problems = new ArrayList<>();
            if (!duplicates.isEmpty()) {
                problems.add(duplicates.size() + " identifier(s) configured twice: " + String.join("; ", duplicates));
            }
            if (!invalidInstances.isEmpty()) {
                problems.add(invalidInstances.size() + " field(s) with instances that do not fit together: "
                        + String.join("; ", invalidInstances));
            }
            if (!brokenReferences.isEmpty()) {
                problems.add(brokenReferences.size() + " reference(s) to something not configured: "
                        + String.join("; ", brokenReferences));
            }
            throw new IllegalStateException(String.join(" | ", problems));
        }
    }

    // A form field in a condition: ['<form title>']['<field label>'].
    private static final Pattern CONDITION_REFERENCE = Pattern.compile("\\['([^']+)']\\['([^']+)']");

    /**
     * The form fields a condition refers to - {@code ['<form>']['<label>']} -
     * that are not configured, as {@code form.label}. A condition on a missing
     * field would never hold, without any error.
     */
    public List<String> findMissingReferences(String condition) {
        return findReferences(condition).stream()
                .filter(reference -> fetchFormFieldConfig(reference.form(), reference.label()) == null)
                .map(ConditionReference::toString)
                .toList();
    }

    /** A form field a condition refers to. */
    private record ConditionReference(String form, String label) {
        @Override
        public String toString() {
            return form + "." + label;
        }
    }

    private static List<ConditionReference> findReferences(String condition) {
        return condition == null ? List.of() : CONDITION_REFERENCE.matcher(condition).results()
                .map(match -> new ConditionReference(match.group(1), match.group(2)))
                .toList();
    }

    /**
     * References within the forms to something not configured: a field's block
     * or group that its form does not define, a layout row naming a field its
     * form does not have, a condition on a field that does not exist (in any
     * form). Checked once all forms are loaded, since conditions can refer to
     * other forms.
     */
    private List<String> findBrokenReferences() {
        List<String> result = new ArrayList<>();
        new TreeMap<>(formTitleFileMap).forEach((formTitle, file) -> {
            String where = file.getFileName() + ", form '" + formTitle + "'";
            Map<String, FormFieldConfig> fields = formTitleLabelFieldMap.getOrDefault(formTitle, Map.of());
            Map<String, Integer> order = formTitleLabelOrderMap.getOrDefault(formTitle, Map.of());
            // Every instance of a label: each has its own block, groups and condition.
            formTitleLabelInstancesMap.getOrDefault(formTitle, Map.of()).values().stream()
                    .flatMap(List::stream)
                    .sorted(Comparator.comparing(field -> order.getOrDefault(field.getLabel(), Integer.MAX_VALUE)))
                    .forEach(field -> {
                        String fieldWhere = where + ", field '" + field.getLabel() + "'";
                        if (field.getBlock() != null && fetchBlock(formTitle, field.getBlock()) == null) {
                            result.add("block '" + field.getBlock() + "' in " + fieldWhere + " is not defined in the form");
                        }
                        Stream.ofNullable(field.getGroups()).flatMap(Arrays::stream)
                                .filter(group -> fetchGroup(formTitle, group) == null)
                                .forEach(group -> result.add("group '" + group + "' in " + fieldWhere
                                        + " is not defined in the form"));
                        findMissingReferences(field.getCondition()).forEach(reference -> result.add(
                                "condition in " + fieldWhere + " refers to field " + reference + ", which does not exist"));
                    });
            formTitleLayoutsMap.getOrDefault(formTitle, List.of()).stream()
                    .flatMap(layout -> Stream.ofNullable(layout.rows()).flatMap(List::stream))
                    .flatMap(row -> Stream.ofNullable(row.fields()).flatMap(List::stream))
                    .filter(label -> !fields.containsKey(label))
                    .forEach(label -> result.add("layout row in " + where + " names field '" + label
                            + "', which the form does not have"));
        });
        return result;
    }

    /** A block of a form, or null. */
    public FormFieldBlock fetchBlock(String formTitle, String blockLabel) {
        return formTitleBlockMap.getOrDefault(formTitle, Map.of()).get(blockLabel);
    }

    /** A group's display metadata within a form, or null. */
    public DisplayMetadata fetchGroup(String formTitle, String group) {
        return formTitleGroupMap.getOrDefault(formTitle, Map.of()).get(group);
    }

    /** Whether this form is marked inactive - see FormMetadataConfig.active. */
    public boolean isFormInactive(String formTitle) {
        return inactiveFormTitles.contains(formTitle);
    }

    private void loadConfigFile(Path configFile, ObjectMapper objectMapper) {
        try {
            FormMetadataConfig formMetadataConfig =
                    objectMapper.readValue(configFile.toFile(), FormMetadataConfig.class);
            Path otherFile = formTitleFileMap.putIfAbsent(formMetadataConfig.getTitle(), configFile);
            if (otherFile != null) {
                duplicates.add("form title '" + formMetadataConfig.getTitle() + "' in " + otherFile.getFileName()
                        + " and " + configFile.getFileName() + " (one file per form)");
                return;
            }
            formTitleJsonMap.put(formMetadataConfig.getTitle(), objectMapper.readTree(configFile.toFile()));
            if (!formMetadataConfig.isActive()) {
                inactiveFormTitles.add(formMetadataConfig.getTitle());
            }
            validateUniqueness(formMetadataConfig, configFile);

            validateContextualInformation(formMetadataConfig, configFile);
            validateFieldTypes(formMetadataConfig, configFile);

            // Title metadata
            formTitleDisplaMetadataMap.put(
                    formMetadataConfig.getTitle(),
                    formMetadataConfig.fetchDisplayMetadata()
            );

            // Layouts
            List<FormFieldLayout> layouts = formTitleLayoutsMap.computeIfAbsent(
                    formMetadataConfig.getTitle(), _ -> new ArrayList<>());
            if (formMetadataConfig.getLayouts() != null) {
                layouts.addAll(Arrays.asList(formMetadataConfig.getLayouts()));
            }

            // Group metadata
            if (formMetadataConfig.getGroups() != null) {
                formTitleGroupMap.put(formMetadataConfig.getTitle(), formMetadataConfig.getGroups());
            }

            // Fields + order
            String title = formMetadataConfig.getTitle();
            Map<String, FormFieldConfig> fieldMap = formTitleLabelFieldMap.computeIfAbsent(title, _ -> new HashMap<>());
            Map<String, Integer> orderMap = formTitleLabelOrderMap.computeIfAbsent(title, _ -> new HashMap<>());
            Map<String, List<FormFieldConfig>> instancesMap =
                    formTitleLabelInstancesMap.computeIfAbsent(title, _ -> new LinkedHashMap<>());

            FormFieldConfig[] fields = Optional.ofNullable(formMetadataConfig.getFields())
                    .orElseGet(() -> new FormFieldConfig[0]);
            Arrays.stream(fields).forEach(field ->
                    instancesMap.computeIfAbsent(field.getLabel(), _ -> new ArrayList<>()).add(field));
            // A label configured several times keeps the first instance and its position.
            instancesMap.forEach((label, instances) -> {
                fieldMap.put(label, instances.getFirst());
                orderMap.put(label, orderMap.size() + 1);
            });

            // Block metadata
            if (formMetadataConfig.getBlocks() != null) {
                Map<String, FormFieldBlock> blocks =
                        formTitleBlockMap.computeIfAbsent(formMetadataConfig.getTitle(), _ -> new HashMap<>());
                Arrays.stream(formMetadataConfig.getBlocks()).forEach(fieldBlock ->
                        blocks.put(fieldBlock.getLabel(), fieldBlock));
            }

            log.info(
                    "Loaded {} form fields from {}",
                    fields.length,
                    configFile.getFileName()
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read form config file " + configFile + ": " + e.getMessage(), e);
        }
    }

    /**
     * Identifiers that must be unique: block labels within the form,
     * allowed-value labels within their field, and FIXED labels across all
     * forms. A field label may repeat within the form only as instances (see
     * validateInstances). (Group ids are keys of one JSON object, so a duplicate
     * is already a parse error; form titles are checked per file.)
     */
    private void validateUniqueness(FormMetadataConfig form, Path configFile) {
        String where = configFile.getFileName() + ", form '" + form.getTitle() + "'";
        FormFieldConfig[] fields = Optional.ofNullable(form.getFields()).orElseGet(() -> new FormFieldConfig[0]);
        Arrays.stream(fields)
                .filter(field -> field.getLabel() != null)
                .collect(Collectors.groupingBy(FormFieldConfig::getLabel, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .filter(labelInstances -> labelInstances.getValue().size() > 1)
                .forEach(labelInstances -> validateInstances(
                        form.getTitle(), labelInstances.getKey(), labelInstances.getValue(), where));
        for (FormFieldConfig field : fields) {
            if (field.getAllowedValues() != null) {
                findDuplicates(Arrays.stream(field.getAllowedValues()).map(value -> value.getLabel()).toList())
                        .forEach(value -> duplicates.add("allowed value '" + value + "' in " + where
                                + ", field '" + field.getLabel() + "'"));
            }
            if (field.getFieldType() == FormFieldType.FIXED && field.getLabel() != null) {
                String otherForm = fixedLabelFormTitleMap.putIfAbsent(field.getLabel(), form.getTitle());
                // Instances of a FIXED field within one form are allowed, the same label in another form is not.
                if (otherForm != null && !otherForm.equals(form.getTitle())) {
                    duplicates.add("FIXED field label '" + field.getLabel() + "' in forms '" + otherForm
                            + "' and '" + form.getTitle() + "'");
                }
            }
        }
        if (form.getBlocks() != null) {
            findDuplicates(Arrays.stream(form.getBlocks()).map(FormFieldBlock::getLabel).toList())
                    .forEach(block -> duplicates.add("block label '" + block + "' in " + where));
        }
    }

    /**
     * A label configured more than once: its instances are tried in order and the first whose condition holds is
     * used, so every instance but the last needs a condition (the last one may be the default). They describe one
     * stored value, so what defines that value must be the same in all of them. A condition referring to the field
     * itself would choose the instance by the value that instance shows.
     */
    private void validateInstances(String formTitle, String label, List<FormFieldConfig> instances, String where) {
        String fieldWhere = "field label '" + label + "' in " + where;
        if (instances.subList(0, instances.size() - 1).stream()
                .anyMatch(instance -> instance.getCondition() == null || instance.getCondition().isBlank())) {
            duplicates.add(fieldWhere + " (configured " + instances.size()
                    + " times: every instance but the last needs a condition)");
        }
        FormFieldConfig first = instances.getFirst();
        List<String> differences = SHARED_INSTANCE_ATTRIBUTES.stream()
                .filter(attribute -> instances.stream().anyMatch(instance ->
                        !Objects.equals(attribute.getValue().apply(first), attribute.getValue().apply(instance))))
                .map(Map.Entry::getKey)
                .toList();
        if (!differences.isEmpty()) {
            invalidInstances.add(fieldWhere + ": its instances differ in " + String.join(", ", differences)
                    + ", which must be the same in all of them");
        }
        ConditionReference itself = new ConditionReference(formTitle, label);
        IntStream.range(0, instances.size())
                .filter(i -> findReferences(instances.get(i).getCondition()).contains(itself))
                .forEach(i -> invalidInstances.add(
                        fieldWhere + ": the condition of instance " + (i + 1) + " refers to the field itself"));
    }

    // What defines a field's stored value: the same in all instances of a label.
    private static final List<Map.Entry<String, Function<FormFieldConfig, Object>>> SHARED_INSTANCE_ATTRIBUTES = List.of(
            Map.entry(ProjectManagerConst.FORM_CONFIG_DATA_TYPE, FormFieldConfig::getDataType),
            Map.entry(ProjectManagerConst.FORM_CONFIG_FIELD_TYPE, FormFieldConfig::getFieldType),
            Map.entry(ProjectManagerConst.FORM_CONFIG_MULTIPLE, FormFieldConfig::isMultiple),
            Map.entry(ProjectManagerConst.FORM_CONFIG_BLOCK, FormFieldConfig::getBlock),
            Map.entry(ProjectManagerConst.FORM_CONFIG_AS_FILE, FormFieldConfig::getAsFile));

    private static Set<String> findDuplicates(List<String> values) {
        Set<String> seen = new HashSet<>();
        Set<String> result = new LinkedHashSet<>();
        values.stream().filter(Objects::nonNull).filter(value -> !seen.add(value)).forEach(result::add);
        return result;
    }

    private void validateFieldTypes(FormMetadataConfig form, Path configFile) {
        if (form.getFields() == null) {
            return;
        }
        Arrays.stream(form.getFields()).forEach(field -> {
            field.validateDisplayFormat();
            if (field.getFieldType() == null) {
                throw new IllegalArgumentException(
                        "Invalid form configuration in " + configFile + " at form '"
                                + form.getTitle() + "', field '" + field.getLabel()
                        + "': field_type must be DYNAMIC or FIXED");
            }
            if (field.getPlaceholder() != null && !field.getPlaceholder().isBlank()
                    && field.getDataType() != DataType.STRING
                    && field.getDataType() != DataType.LONG_STRING) {
                throw new IllegalArgumentException(
                        "Invalid form configuration in " + configFile + " at form '"
                                + form.getTitle() + "', field '" + field.getLabel()
                                + "': placeholder is only supported for STRING and LONG_STRING fields");
            }
        });
    }

    private void validateContextualInformation(FormMetadataConfig form, Path configFile) {
        validateContextualInformation(form, configFile, "form '" + form.getTitle() + "'");

        if (form.getFields() != null) {
            Arrays.stream(form.getFields()).forEach(field -> validateContextualInformation(
                    field,
                    configFile,
                    "form '" + form.getTitle() + "', field '" + field.getLabel() + "'"));
        }

        if (form.getBlocks() != null) {
            Arrays.stream(form.getBlocks()).forEach(block -> validateContextualInformation(
                    block,
                    configFile,
                    "form '" + form.getTitle() + "', block '" + block.getLabel() + "'"));
        }
    }

    private void validateContextualInformation(
            ContextualDisplayMetadata metadata, Path configFile, String location) {
        validateDisplayInfo(metadata.getPreInfo(), configFile, location + ".pre_info");
        validateDisplayInfo(metadata.getPostInfo(), configFile, location + ".post_info");
    }

    private void validateDisplayInfo(DisplayInfo info, Path configFile, String location) {
        if (info != null && info.getProjectStates() != null && info.getProjectStates().isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid form configuration in " + configFile + " at " + location
                            + ": project_states must not be empty; omit project_states to allow all states");
        }
    }

    public FormFieldConfig fetchFormFieldConfig(String formTitle, String formLabel) {
        return formTitleLabelFieldMap.getOrDefault(formTitle, new HashMap<>()).get(formLabel);
    }

    /** Every instance of a field, in configuration order; empty if the field is not configured. */
    public List<FormFieldConfig> fetchFormFieldConfigs(String formTitle, String formLabel) {
        return formTitleLabelInstancesMap.getOrDefault(formTitle, Map.of()).getOrDefault(formLabel, List.of());
    }

    public List<FormFieldConfig> fetchFieldsByTitleAndBlock(String title, String block) {
        return formTitleLabelFieldMap.getOrDefault(title, Map.of()).values().stream()
                .filter(config -> Objects.equals(config.getBlock(), block))
                .toList();
    }

}
