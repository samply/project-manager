package de.samply.beam;

public class BeamServiceException extends RuntimeException {
    public BeamServiceException(String message) {
        super(message);
    }

    public BeamServiceException(Throwable cause) {
        super(cause);
    }

}
