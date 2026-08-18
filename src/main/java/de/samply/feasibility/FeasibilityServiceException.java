package de.samply.feasibility;

public class FeasibilityServiceException extends RuntimeException {

    public FeasibilityServiceException(String message) {
        super(message);
    }

    public FeasibilityServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
