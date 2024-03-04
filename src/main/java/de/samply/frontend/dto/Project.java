package de.samply.frontend.dto;

import de.samply.project.ProjectType;
import de.samply.project.state.ProjectState;
import de.samply.query.OutputFormat;
import de.samply.query.QueryFormat;

import lombok.Data;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Data
public class Project {

    private String code;
    private String creatorEmail;
    private Instant createdAt;
    private LocalDate expiresAt;
    private Instant archivedAt;
    private Instant modifiedAt;
    private ProjectState state;
    private ProjectType type;
    private String query;
    private String humanReadable;
    private QueryFormat queryFormat;
    private OutputFormat outputFormat;
    private String templateId;
    private String label;
    private String description;
    private String explorerUrl;
    private String queryContext;
    private boolean isCustomConfig;

    @SneakyThrows
    public void setFieldsFromMap(Map<String, String> fieldValues) {
        for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
            Field field = getClass().getDeclaredField(entry.getKey());
            field.setAccessible(true);
            setField(field, entry.getValue());
        }
    }

    @SneakyThrows
    private void setField(Field field, String value) {
        Class<?> fieldType = field.getType();
        if (fieldType.equals(String.class)) {
            field.set(this, value);
        } else if (fieldType.equals(Instant.class)) {
            field.set(this, Instant.parse(value));
        } else if (fieldType.equals(LocalDate.class)) {
            field.set(this, LocalDate.parse(value));
        } else if (fieldType.isEnum()) {
            field.set(this, Enum.valueOf((Class<Enum>) fieldType, value));
        }
    }

}
