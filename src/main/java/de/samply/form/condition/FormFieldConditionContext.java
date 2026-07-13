package de.samply.form.condition;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.samply.frontend.dto.FormField;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds the SpEL evaluation contexts used for form-field visibility conditions.
 * <p>
 * A condition is evaluated against a set of fields where:
 * non-block fields are always present, and each block contributes exactly one
 * concrete block instance at a time. When multiple blocks have multiple
 * instances, this produces several valid combinations, so a single field may
 * need to be evaluated against multiple {@link StandardEvaluationContext}s.
 * <p>
 * Contexts are stored by block-instance key:
 * <ul>
 *   <li>{@code NonBlock}: every generated context</li>
 *   <li>{@code <block><instance>}: every context that contains that concrete
 *   block instance, for example {@code A1} or {@code B3}</li>
 * </ul>
 */
public class FormFieldConditionContext {

    private static final String NON_BLOCK = "NonBlock";
    private static final TypeReference<HashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<StandardEvaluationContext>> context = new HashMap<>();

    public FormFieldConditionContext(Collection<FormField> formFields) {
        createContext(formFields);
    }

    private void createContext(Collection<FormField> formFields) {
        Map<String, List<FormField>> fieldsByBlock = formFields.stream()
                .collect(Collectors.groupingBy(this::fetchBlock, LinkedHashMap::new, Collectors.toList()));

        List<FormField> nonBlockFields = fieldsByBlock.getOrDefault(NON_BLOCK, List.of());

        Map<String, List<Map.Entry<String, List<FormField>>>> blockFamilies = fieldsByBlock.entrySet().stream()
                .filter(entry -> !NON_BLOCK.equals(entry.getKey()))
                .collect(Collectors.groupingBy(
                        entry -> fetchBlockFamily(entry.getKey()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<List<Map.Entry<String, List<FormField>>>> perBlockInstances = new ArrayList<>(blockFamilies.values()).stream()
                .toList();

        for (List<Map.Entry<String, List<FormField>>> combination : cartesianProduct(perBlockInstances)) {
            List<FormField> combinedFields = new ArrayList<>(nonBlockFields);
            List<String> keys = new ArrayList<>(List.of(NON_BLOCK));

            for (Map.Entry<String, List<FormField>> instanceFields : combination) {
                keys.add(instanceFields.getKey());
                combinedFields.addAll(instanceFields.getValue());
            }

            StandardEvaluationContext evaluationContext = buildEvaluationContext(combinedFields);
            keys.forEach(key -> context.computeIfAbsent(key, _ -> new ArrayList<>()).add(evaluationContext));
        }
    }

    private String fetchBlockFamily(String blockKey) {
        int lastSeparator = blockKey.lastIndexOf('.');
        return lastSeparator < 0 ? blockKey : blockKey.substring(0, lastSeparator);
    }

    private StandardEvaluationContext buildEvaluationContext(List<FormField> fields) {
        Map<String, Object> root = new LinkedHashMap<>();

        for (FormField formField : fields) {
            @SuppressWarnings("unchecked")
            Map<String, Object> titleMap = (Map<String, Object>) root.computeIfAbsent(
                    formField.title(),
                    _ -> new LinkedHashMap<>()
            );
            titleMap.put(formField.label(), serializeFormField(formField));
        }

        return new StandardEvaluationContext(root);
    }

    private static <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> result = new ArrayList<>(List.of(List.of()));
        for (List<T> list : lists) {
            result = result.stream()
                    .flatMap(partial -> list.stream().map(item -> {
                        List<T> next = new ArrayList<>(partial);
                        next.add(item);
                        return next;
                    }))
                    .toList();
        }
        return result;
    }

    private Map<String, Object> serializeFormField(FormField formField) {
        return objectMapper.convertValue(formField, MAP_TYPE);
    }

    public Collection<StandardEvaluationContext> getContext(FormField formField) {
        return context.get(fetchBlock(formField));
    }

    private String fetchBlock(FormField formField) {
        if (formField.block() == null) {
            return NON_BLOCK;
        }
        // A block context is shared by all fields belonging to the same block instance.
        return formField.title() + "." + formField.block() +
                ((formField.blockInstance() != null) ? "." + formField.blockInstance() : "");
    }


}
