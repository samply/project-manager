package de.samply.user.roles;

public enum ProjectRole {
    CREATOR,
    DEVELOPER,
    PILOT,
    FINAL,
    BRIDGEHEAD_ADMIN,
    PROJECT_MANAGER_ADMIN,
    DEFAULT // This role is intended for email thymeleaf templates, not for constraints
}
