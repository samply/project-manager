package de.samply.user.roles;

public enum ProjectRole {
    CREATOR, // This role is intended for restrictions. It shouldn't appear in a project bridgehead user, as the creator is not associated with a specific bridgehead but to the whole project.
    DEVELOPER,
    PILOT,
    FINAL,
    BRIDGEHEAD_ADMIN,
    PROJECT_MANAGER_ADMIN,
    // The people of a site's local approval: they have no access to the project manager, they only get emails. Like
    // DEFAULT, intended for the email templates, not for constraints
    BRIDGEHEAD_APPROVAL_CONTACT,
    DEFAULT // This role is intended for email thymeleaf templates, not for constraints
}
