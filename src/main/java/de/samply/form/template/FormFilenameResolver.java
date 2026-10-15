package de.samply.form.template;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FormFilenameResolver {

    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("\\$\\{([^}]+)}");

    /**
     * Resolves a filename template using the given context.
     * Only variables defined in FormFilenameKey are allowed.
     * Missing variables are replaced with an empty string.
     *
     * @param template The filename template, e.g. "samples-${project-code}-${timestamp}.pdf"
     * @param context  The variable context map, e.g. Map.of("project-code", "ABC123")
     * @return The resolved filename
     */
    public static String resolve(@NotNull String template, @NotNull Map<String, String> context) {
        Objects.requireNonNull(template, "template must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);

            // Fetch the value from the enum + context, or "" if missing
            String replacement = FormFilenameKey.fetchValue(key, context)
                    .orElse("");

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

}
