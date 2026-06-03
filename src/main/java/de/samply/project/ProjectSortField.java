package de.samply.project;

import java.util.Arrays;

public enum ProjectSortField {
    TITLE("title"), REQUEST_ID("request-id"), PROJECT_CREATOR("project-creator"),
    STATUS("status"), CREATED("created"), MODIFIED_AT("modified-at");

    private final String value;

    ProjectSortField(String value) { this.value = value; }

    public static ProjectSortField fromValue(String value) {
        return Arrays.stream(values()).filter(field -> field.value.equalsIgnoreCase(value))
                .findFirst().orElse(MODIFIED_AT);
    }
}
