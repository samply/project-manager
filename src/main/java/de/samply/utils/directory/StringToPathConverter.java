package de.samply.utils.directory;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.file.Path;

/**
 * Spring converter that transforms configured String values into normalized {@link Path} instances.
 *
 * <p>This converter is used for filesystem paths configured through application properties
 * or environment variables. It ensures that all injected {@code Path} values use a consistent
 * normalization strategy across the application.</p>
 *
 * <p>The conversion performs the following steps:</p>
 * <ul>
 *     <li>Checks that the configured value is not null or blank.</li>
 *     <li>Converts path separators to the separator used by the current operating system.</li>
 *     <li>Creates a {@link Path} instance.</li>
 *     <li>Normalizes the path by removing redundant elements such as {@code .} and resolving
 *         {@code ..} where possible.</li>
 * </ul>
 *
 * <p>This converter is registered globally in Spring's conversion service and therefore
 * applies to all String-to-Path conversions.</p>
 */
public class StringToPathConverter implements Converter<String, Path> {

    @Override
    public Path convert(String source) {
        Assert.hasText(source, "Path value must not be null or blank");

        String normalizedSeparators = source
                .replace("\\", File.separator)
                .replace("/", File.separator);

        return Path.of(normalizedSeparators).normalize();
    }

}