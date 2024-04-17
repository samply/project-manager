package de.samply.exporter;

public class ExporterServiceException extends RuntimeException {

    public ExporterServiceException(String message) {
        super(message);
    }

    public ExporterServiceException(Throwable cause) {
        super(cause);
    }

}
