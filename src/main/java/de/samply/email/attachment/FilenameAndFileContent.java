package de.samply.email.attachment;

public record FilenameAndFileContent(
        String filename,
        byte[] fileContent
) {
}
