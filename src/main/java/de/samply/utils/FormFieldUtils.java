package de.samply.utils;

import de.samply.frontend.dto.FormField;
import jakarta.validation.constraints.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class FormFieldUtils {

    // Comparator to sort FormFields by order -> title -> label
    public static final Comparator<FormField> FORM_FIELD_COMPARATOR =
            Comparator.comparing(FormField::title, Comparator.nullsLast(String::compareTo))
                    .thenComparing(FormFieldUtils::compareByInstanceIfSameBlock)
                    .thenComparing(FormField::order, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(FormField::label, Comparator.nullsLast(String::compareTo));

    // Only compares by blockInstance when both fields belong to the SAME
    // non-null block; otherwise it's a no-op (0), letting order decide -
    // this is what allows a non-block/different-block field to sort by its
    // own order instead of being grouped with an unrelated block.
    private static int compareByInstanceIfSameBlock(FormField f1, FormField f2) {
        boolean sameBlock = f1.block() != null && Objects.equals(f1.block(), f2.block());
        return sameBlock
                ? Comparator.nullsLast(Integer::compareTo).compare(f1.blockInstance(), f2.blockInstance())
                : 0;
    }


    public static Collector<FormField, ?, LinkedHashMap<String, FormField>>
    formFieldMapCollector() {

        return Collectors.toMap(
                FormFieldUtils::fetchFormFieldKey,
                Function.identity(),
                (_, newValue) -> newValue, // the last one wins
                LinkedHashMap::new
        );
    }

    public static String fetchFormFieldKey(@NotNull FormField formField) {
        return formField.title() + formField.label() + (formField.blockInstance() != null ? formField.blockInstance() : "");
    }

}
