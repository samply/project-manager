package de.samply.utils;

import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DirectoryUtils {

    public static String fetchExternalTemplateDirectory(String environmentVariable) throws FileNotFoundException {
        if (StringUtils.hasText(environmentVariable) && Files.isDirectory(Path.of(environmentVariable))) {
            return (environmentVariable.endsWith("/") || environmentVariable.endsWith("\\")) ?
                    environmentVariable : environmentVariable + "/";
        } else {
            throw new FileNotFoundException("External Templates Directory not set or set to incorrect directory: " + environmentVariable);
        }
    }

}
