package de.samply.app;

public class ProjectManagerControllerException extends Exception {
    public ProjectManagerControllerException() {
    }

    public ProjectManagerControllerException(String message) {
        super(message);
    }

    public ProjectManagerControllerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectManagerControllerException(Throwable cause) {
        super(cause);
    }

    public ProjectManagerControllerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
