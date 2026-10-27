package de.samply.user.roles;

public enum OrganisationRole {
    RESEARCHER,
    BRIDGEHEAD_ADMIN,
    PROJECT_MANAGER_ADMIN,
    // Approves requests in REVIEW and CENTRAL_APPROVAL; sees every request except drafts (CENTRAL_APPROVAL_COMMITTEE_GROUPS)
    CENTRAL_APPROVAL_COMMITTEE
}
