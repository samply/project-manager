package de.samply.utils.directory;

import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;

public class StringToEnsuredDirectoryConverter
        implements Converter<String, EnsuredDirectory> {

    private final StringToPathConverter pathConverter;

    public StringToEnsuredDirectoryConverter(StringToPathConverter pathConverter) {
        this.pathConverter = pathConverter;
    }

    @Override
    public EnsuredDirectory convert(@NonNull String source) {
        return new EnsuredDirectory(pathConverter.convert(source));
    }

}
