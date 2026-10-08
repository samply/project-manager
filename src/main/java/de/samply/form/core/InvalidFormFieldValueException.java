package de.samply.form.core;

// A form field value that does not match the data type of its field.
public class InvalidFormFieldValueException extends IllegalArgumentException {

    public InvalidFormFieldValueException(String message) {
        super(message);
    }

}
