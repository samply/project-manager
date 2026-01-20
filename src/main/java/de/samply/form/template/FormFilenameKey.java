package de.samply.form.template;

import de.samply.app.ProjectManagerConst;
import de.samply.utils.DateUtils;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum FormFilenameKey {

    PROJECT_CODE("project-code"),
    TIMESTAMP("timestamp") {
        @Override
        public Optional<String> fetchValue(Map<String, String> context) {
            return Optional.of(DateUtils.fetchCurrentDate(ProjectManagerConst.FORM_FILENAME_TIMESTAMP_FORMAT));
        }
    };

    private final String text;

    FormFilenameKey(String text) {
        this.text = text;
    }

    private static final Map<String, FormFilenameKey> BY_TEXT =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(
                            FormFilenameKey::getText,
                            Function.identity()
                    ));

    public static Optional<FormFilenameKey> fromText(String text) {
        return Optional.ofNullable(BY_TEXT.get(text));
    }

    public Optional<String> fetchValue(Map<String, String> context) {
        return Optional.ofNullable(context != null ? context.get(text) : null);
    }

    public static Optional<String> fetchValue(String key, Map<String, String> context) {
        return fromText(key).flatMap(k -> k.fetchValue(context));
    }


}
