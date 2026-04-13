package de.samply.form.template;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum ProjectContextKey {

    PROJECT_CODE("project-code"),
    CREATOR_BRIDGEHEADS("creator-bridgeheads"),
    CREATOR_AFFILIATIONS("creator-affiliations"),
    CREATOR_NAME("creator-name"),
    CREATOR_EMAIL("creator-email"),
    PROJECT_TITLE("project-title"),
    PROJECT_DESCRIPTION("project-description"),
    PROJECT_CREATION_DATE("project-creation-date"),
    ETHICAL_APPROVAL("ethical-approval"),;

    private final String text;

    private static final Map<String, ProjectContextKey> BY_TEXT =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            ProjectContextKey::getText,
                            Function.identity()
                    ));

    ProjectContextKey(String text) {
        this.text = text;
    }

    public static ProjectContextKey fromText(String text) {
        return BY_TEXT.get(text);
    }

}
