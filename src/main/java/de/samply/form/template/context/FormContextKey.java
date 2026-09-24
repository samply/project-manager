package de.samply.form.template.context;

import lombok.Getter;

@Getter
public enum FormContextKey {

    FIELDS("fields"),
    LAYOUTS("layouts"),
    DOCUMENT("document"),
    DATA_TYPE_CLASS("DataType"),
    CURRENT_DATE("currentDate");

    private final String text;

    FormContextKey(String text) {
        this.text = text;
    }

}
