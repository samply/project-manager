package de.samply.utils.directory;

import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record EnsuredDirectory(Path path) {

    public EnsuredDirectory {
        Assert.notNull(path, "Path must not be null");
        try {
            Files.createDirectories(path);

            if (!Files.isDirectory(path)) {
                throw new IllegalArgumentException(
                        "Path is not a directory: " + path);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot create directory: " + path, e);
        }
    }

}
