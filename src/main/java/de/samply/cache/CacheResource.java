package de.samply.cache;

/** Resource categories for which a cache policy can be configured. */
public enum CacheResource {

    /** Static files served by the backend under /assets/. */
    BACKEND_ASSETS,

    /** Backend-provided variables used by the frontend at runtime. */
    FRONTEND_VARIABLES,

    /** API responses containing the project dashboard listing. */
    PROJECT_DASHBOARD,

    /** API responses containing details of a project. */
    PROJECT_DETAIL,

    /** Form metadata and field configuration responses. */
    FORM_METADATA,

    /** Relatively stable reference data such as states, formats, and project types. */
    REFERENCE_DATA,

    /** User roles and permission responses. */
    USER_ROLES,

    /** Responses describing available actions or modules. */
    ACTION_AVAILABILITY,

    /** Feasibility and statistics responses. */
    FEASIBILITY_STATISTICS,

    /** Project and bridgehead result responses. */
    PROJECT_RESULTS,

    /** Project documents, descriptions, scripts, and associated metadata. */
    PROJECT_DOCUMENTS,

    /** Project bridgehead and execution responses. */
    PROJECT_BRIDGEHEAD_EXECUTIONS,

    /** User notification responses. */
    NOTIFICATIONS,

    /** User directories, autocomplete results, and mailing-list administration. */
    USER_DATA,

    /** Other authenticated GET responses not assigned to a specific category. */
    AUTHENTICATED_GET,

    /** Public informational GET responses. */
    PUBLIC_INFORMATION,

    /** Responses from POST, PUT, and DELETE operations. */
    MUTATION_RESPONSES
}
