package de.samply.document;

public class DocumentServiceException extends Exception {

    public DocumentServiceException() {
    }

    public DocumentServiceException(String message) {
        super(message);
    }

    public DocumentServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentServiceException(Throwable cause) {
        super(cause);
    }

    public DocumentServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
