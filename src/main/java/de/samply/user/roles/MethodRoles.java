package de.samply.user.roles;

public record MethodRoles(
        String httpMethod,
        String[] roles
) {
}
