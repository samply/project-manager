package de.samply.utils.directory;

import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

public class NormalizingPathEditor extends PropertyEditorSupport {

    private final StringToPathConverter converter = new StringToPathConverter();

    @Override
    public void setAsText(String text) {
        setValue(StringUtils.hasText(text) ? converter.convert(text) : null);
    }

}