package de.samply.utils.directory;

import org.springframework.util.Assert;

import java.nio.file.Files;
import java.nio.file.Path;

public record ExistingDirectory(Path path) {

    public ExistingDirectory {
        Assert.notNull(path, "Path must not be null");

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    "Directory does not exist: " + path);
        }

    }
}