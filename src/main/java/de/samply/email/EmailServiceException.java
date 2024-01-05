package de.samply.email;

public class EmailServiceException extends Exception {

    public EmailServiceException(String message) {
        super(message);
    }

    public EmailServiceException(Throwable cause) {
        super(cause);
    }

}
