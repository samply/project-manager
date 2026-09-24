package de.samply.frontend;

public record Action(
        String path,
        String method,
        String[] params,
        String explanation,
        String successMessage,
        String errorMessage,
        Integer priority,
        // The endpoint needs a site: it can only be called in a context with a site (not for a project without sites)
        boolean bridgeheadRequired
) {
}
