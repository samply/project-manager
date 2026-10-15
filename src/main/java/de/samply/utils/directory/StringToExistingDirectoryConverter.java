package de.samply.utils.directory;

import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;

public class StringToExistingDirectoryConverter
        implements Converter<String, ExistingDirectory> {

    private final StringToPathConverter pathConverter;

    public StringToExistingDirectoryConverter(StringToPathConverter pathConverter) {
        this.pathConverter = pathConverter;
    }

    @Override
    public ExistingDirectory convert(@NonNull String source) {
        return new ExistingDirectory(pathConverter.convert(source));
    }

}
