package de.samply.form.pdf;

import lombok.Getter;

@Getter
public enum FormKey {

    FIELDS("fields"),
    DATA_TYPE_CLASS("DataType");

    private final String text;

    FormKey(String text) {
        this.text = text;
    }

}
