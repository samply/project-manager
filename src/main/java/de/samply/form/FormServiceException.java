package de.samply.form;

public class FormServiceException extends RuntimeException {

    public FormServiceException(String message) {
        super(message);
    }

    public FormServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
