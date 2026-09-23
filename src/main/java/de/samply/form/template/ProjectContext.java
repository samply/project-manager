package de.samply.form.template;


import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProjectContext {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private final Map<ProjectContextKey, String> context;

    public ProjectContext(Map<ProjectContextKey, String> context) {
        this.context = context;
    }

    public FormTemplateFieldConfig resolveProjectContext(FormTemplateFieldConfig original) {
        if (original == null || original.getProjectValue() == null) {
            return original;
        }
        return original.toBuilder().projectValue(resolvePlaceholders(original.getProjectValue())).build();
    }

    // A value that is not set (e.g. a creator without name, a project without
    // title) is "", never "null" - and never a null value, which toMap rejects.
    public Map<String, String> fetchContext() {
        return context
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getText(),
                        e -> Objects.toString(e.getValue(), "")
                ));
    }

    private String resolvePlaceholders(String input) {
        if (input == null) return null;

        Matcher matcher = PLACEHOLDER.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String keyText = matcher.group(1);
            ProjectContextKey key = ProjectContextKey.fromText(keyText);

            // A known key without value resolves to ""; an unknown placeholder
            // (e.g. a typo in the configuration) is left visible as it is.
            String replacement = key != null && context.containsKey(key)
                    ? Objects.toString(context.get(key), "")
                    : null;

            // Matcher.quoteReplacement is required in both branches: appendReplacement
            // treats "$" and "\" in its replacement argument specially (backreferences/
            // escapes), and the fallback text here is "${...}" itself, which otherwise
            // throws (e.g. "named capturing group is missing trailing '}'") instead of
            // simply leaving the placeholder in place when its key has no value.
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(replacement != null ? replacement : matcher.group(0)));
        }

        matcher.appendTail(result);
        return result.toString();
    }

}
