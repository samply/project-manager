package de.samply.user.roles;

public enum ProjectRole {
    CREATOR, // This role is intended for restrictions. It shouldn't appear in a project bridgehead user, as the creator is not associated to an specific bridgehead, but to the whole project.
    DEVELOPER,
    PILOT,
    FINAL,
    BRIDGEHEAD_ADMIN,
    PROJECT_MANAGER_ADMIN,
    DEFAULT // This role is intended for email thymeleaf templates, not for constraints
}
