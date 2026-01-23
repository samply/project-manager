package de.samply.form.template;

import de.samply.form.FormFieldConfig;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectContext {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private final Map<ProjectContextKey, String> context;

    public ProjectContext(Map<ProjectContextKey, String> context) {
        this.context = context;
    }

    public FormFieldConfig resolveProjectContext(FormFieldConfig original) {
        if (original == null || original.getProjectValue() == null) {
            return original;
        }
        return original.toBuilder().projectValue(resolvePlaceholders(original.getProjectValue())).build();
    }

    private String resolvePlaceholders(String input) {
        if (input == null) return null;

        Matcher matcher = PLACEHOLDER.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String keyText = matcher.group(1);
            ProjectContextKey key = ProjectContextKey.fromText(keyText);

            String replacement = key != null ? context.get(key) : null;

            matcher.appendReplacement(result,
                    replacement != null ? Matcher.quoteReplacement(replacement)
                            : matcher.group(0)); // keep placeholder if missing
        }

        matcher.appendTail(result);
        return result.toString();
    }

}
