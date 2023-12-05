package de.samply.project;

public class ProjectEventActionsException extends Exception {

    public ProjectEventActionsException() {    }

    public ProjectEventActionsException(String message) {
        super(message);
    }

    public ProjectEventActionsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectEventActionsException(Throwable cause) {
        super(cause);
    }

    public ProjectEventActionsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
