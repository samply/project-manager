package de.samply.form.template;

import lombok.Getter;

@Getter
public enum FormKey {

    FIELDS("fields"),
    DATA_TYPE_CLASS("DataType"),
    CURRENT_DATE("currentDate");

    private final String text;

    FormKey(String text) {
        this.text = text;
    }

}
