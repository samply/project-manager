package de.samply.utils;

import de.samply.frontend.dto.FormField;

import java.util.Comparator;

public class FormFieldUtils {

    // Comparator to sort FormFields by order -> title -> label
    public static final Comparator<FormField> FORM_FIELD_COMPARATOR =
            Comparator.comparing(FormField::title, Comparator.nullsLast(String::compareTo))
                    .thenComparing(FormField::order, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(FormField::label, Comparator.nullsLast(String::compareTo));

}
