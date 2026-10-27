package de.samply.security;

/**
 * Group names of the identity provider, compared without a leading slash: Keycloak sends full paths
 * ("/projects/x/admin"), other providers (Authentik) plain names. Every group setting (full names and prefixes, not
 * suffixes) and every group of the token goes through {@link #normalize(String)}, so a setting may be written with or
 * without the slash.
 */
public final class GroupNames {

    private GroupNames() {
    }

    public static String normalize(String group) {
        return (group == null) ? "" : group.trim().replaceFirst("^/", "");
    }

}
