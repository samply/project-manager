package de.samply.utils;

import de.samply.frontend.dto.FormField;
import jakarta.validation.constraints.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class FormFieldUtils {

    // Comparator to sort FormFields by order -> title -> label
    public static final Comparator<FormField> FORM_FIELD_COMPARATOR =
            Comparator.comparing(FormField::title, Comparator.nullsLast(String::compareTo))
                    .thenComparing(FormField::order, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(FormField::label, Comparator.nullsLast(String::compareTo));

    public static Collector<FormField, ?, LinkedHashMap<String, FormField>>
    formFieldMapCollector() {

        return Collectors.toMap(
                field -> fetchFormFieldKey(field),
                Function.identity(),
                (_, newValue) -> newValue, // the last one wins
                LinkedHashMap::new
        );
    }

    public static String fetchFormFieldKey(@NotNull FormField formField) {
        return formField.title() + formField.label();
    }

}
