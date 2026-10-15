package de.samply.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectUtils {

    /**
     * Formats a collection of items as a comma-separated string with "and" before the last element.
     * Examples:
     * [T1] -> "T1"
     * [T1, T2] -> "T1 and T2"
     * [T1, T2, T3] -> "T1, T2 and T3"
     *
     * @param items collection of items (enums or strings)
     * @return formatted string
     */
    public static String formatWithCommasAndAnd(Collection<?> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        List<String> list = items.stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        int size = list.size();
        if (size == 1) {
            return list.getFirst();
        }

        return String.join(", ", list.subList(0, size - 1))
                + " and " + list.get(size - 1);
    }

}
