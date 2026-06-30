package de.samply.pdf;

public interface PdfConverter {

    byte[] convert(String content) throws PdfConverterException;

}
