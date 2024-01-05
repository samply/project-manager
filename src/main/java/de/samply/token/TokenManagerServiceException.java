package de.samply.token;

public class TokenManagerServiceException extends Exception {
    public TokenManagerServiceException(Throwable cause) {
        super(cause);
    }

    public TokenManagerServiceException(String message) {
        super(message);
    }
}
