package de.samply.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FileExtension {
    PDF("pdf"),
    HTML("html"),
    JSON("json"),
    XML("xml");

    private final String value;

    FileExtension(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static FileExtension fromValue(String value) {
        for (FileExtension ext : values()) {
            if (ext.value.equalsIgnoreCase(value)) {
                return ext;
            }
        }
        throw new IllegalArgumentException("Unknown file extension: " + value);
    }

}
