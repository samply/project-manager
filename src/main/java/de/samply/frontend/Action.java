package de.samply.frontend;

public record Action(
        String path,
        String method,
        String[] params
) {
}
