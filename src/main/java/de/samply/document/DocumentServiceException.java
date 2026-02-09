package de.samply.document;

public class DocumentServiceException extends Exception {

    public DocumentServiceException(String message) {
        super(message);
    }

    public DocumentServiceException(Throwable cause) {
        super(cause);
    }

}
