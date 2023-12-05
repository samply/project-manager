package de.samply.frontend;

public record Action(
        Site site,
        Module module,
        String path
) {

}
