package de.samply.pdf;


import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;


public class PdfGenerator {

    private final TemplateEngine templateEngine;
    private final PdfConverter pdfConverter;

    public PdfGenerator(TemplateEngine templateEngine, PdfConverter pdfConverter) {
        this.templateEngine = templateEngine;
        this.pdfConverter = pdfConverter;
    }

    public byte[] generatePdf(String templateFilename, Map<String, Object> context) throws PdfGeneratorException {
        try {
            return pdfConverter.convert(templateEngine.process(templateFilename, createContext(context)));
        } catch (PdfConverterException e) {
            throw new PdfGeneratorException(e);
        }
    }

    private Context createContext(Map<String, Object> context) {
        Context result = new Context();
        result.setVariables(context);
        return result;
    }

}
