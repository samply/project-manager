package de.samply.email;

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
    PROJECT_ALL
}
