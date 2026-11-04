package de.samply.email;

import de.samply.user.roles.ProjectRole;

import java.util.Set;

public enum EmailRecipientType {
    SESSION_USER,
    EMAIL_ANNOTATION,
    CREATOR,
    PROJECT_MANAGER_ADMIN,
    CENTRAL_APPROVAL_COMMITTEE,
    ALL_DEVELOPERS,
    ALL_PILOTS,
    ALL_FINALS,
    ALL_BRIDGEHEAD_ADMINS,
    BRIDGEHEAD_ADMIN,
    // Approval contacts of the bridgehead (or of all bridgeheads of the project); not users of the project manager
    BRIDGEHEAD_APPROVAL_CONTACTS,
    BRIDGEHEAD_ADMINS_WHO_HAVE_NOT_ACCEPTED_NOR_REJECTED_THE_PROJECT,
    PROJECT_ALL;

    // The template roles of the emails to these recipients (EmailSenderAspect). Empty: the role depends on the person
    // (the session user, an invited user, everyone of the request), so any template of the email may be used
    public Set<ProjectRole> fetchTemplateRoles() {
        return switch (this) {
            case CREATOR -> Set.of(ProjectRole.CREATOR);
            case PROJECT_MANAGER_ADMIN -> Set.of(ProjectRole.PROJECT_MANAGER_ADMIN);
            case CENTRAL_APPROVAL_COMMITTEE -> Set.of(ProjectRole.CENTRAL_APPROVAL_COMMITTEE);
            case ALL_DEVELOPERS -> Set.of(ProjectRole.DEVELOPER);
            case ALL_PILOTS -> Set.of(ProjectRole.PILOT);
            case ALL_FINALS -> Set.of(ProjectRole.FINAL);
            case ALL_BRIDGEHEAD_ADMINS, BRIDGEHEAD_ADMIN, BRIDGEHEAD_ADMINS_WHO_HAVE_NOT_ACCEPTED_NOR_REJECTED_THE_PROJECT ->
                    Set.of(ProjectRole.BRIDGEHEAD_ADMIN);
            case BRIDGEHEAD_APPROVAL_CONTACTS -> Set.of(ProjectRole.BRIDGEHEAD_APPROVAL_CONTACT);
            case SESSION_USER, EMAIL_ANNOTATION, PROJECT_ALL -> Set.of();
        };
    }
}
