package de.samply.email;

public class EmailServiceException extends RuntimeException {

    public EmailServiceException(String message) {
        super(message);
    }

    public EmailServiceException(Throwable cause) {
        super(cause);
    }

}
