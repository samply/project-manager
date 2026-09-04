package de.samply.frontend;

/** Localized messages and explanation priority for an active frontend action. */
public record ResolvedActionMessages(
        String explanation,
        String successMessage,
        String errorMessage,
        Integer priority
) {
}
