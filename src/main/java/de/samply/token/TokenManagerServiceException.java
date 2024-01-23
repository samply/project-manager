package de.samply.token;

public class TokenManagerServiceException extends RuntimeException {
    public TokenManagerServiceException(Throwable cause) {
        super(cause);
    }

    public TokenManagerServiceException(String message) {
        super(message);
    }
}
