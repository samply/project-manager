package de.samply.exporter;

public class ExporterServiceException extends Exception{

    public ExporterServiceException(String message) {
        super(message);
    }

    public ExporterServiceException(Throwable cause) {
        super(cause);
    }

}
