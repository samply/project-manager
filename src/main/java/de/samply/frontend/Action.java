package de.samply.frontend;

import java.util.List;

public record Action(
        String path,
        String method,
        String[] params,
        String explanation,
        String successMessage,
        String errorMessage,
        Integer priority,
        // The endpoint needs a site: it can only be called in a context with a site (not for a project without sites)
        boolean bridgeheadRequired,
        // Who gets an email when the action succeeds (EmailRecipientType names), so the frontend can say so before
        // the user confirms. Empty if the action sends no email.
        List<String> emailRecipients
) {
}
