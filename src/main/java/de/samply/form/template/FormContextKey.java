package de.samply.form.template;

import lombok.Getter;

@Getter
public enum FormContextKey {

    FIELDS("fields"),
    DATA_TYPE_CLASS("DataType"),
    CURRENT_DATE("currentDate");

    private final String text;

    FormContextKey(String text) {
        this.text = text;
    }

}
