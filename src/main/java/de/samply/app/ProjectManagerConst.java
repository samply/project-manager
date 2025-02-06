package de.samply.app;

public class ProjectManagerConst {

    public final static String APP_NAME = "Project Manager";

    // R-Studio Group Implementations
    public final static String RSTUDIO_GROUP_KEYCLOAK_IMPLEMENTATION = "KEYCLOAK";

    // Keycloak paths
    public final static String FETCH_USER_ID_KEYCLOAK_PATH = "/admin/realms/{realm}/users?email={email}";
    public final static String FETCH_GROUP_ID_KEYCLOAK_PATH = "/admin/realms/{realm}/groups?search={group}";
    public final static String CHANGE_USER_GROUP_KEYCLOAK_PATH = "/admin/realms/{realm}/users/{user-id}/groups/{group-id}";
    public final static String FETCH_TOKEN_KEYCLOAK_PATH = "/realms/{realm}/protocol/openid-connect/token";

    // Keycloak parameters
    public final static String CLIENT_ID_KEYCLOAK_PARAM = "client_id";
    public final static String CLIENT_SECRET_KEYCLOAK_PARAM = "client_secret";
    public final static String GRANT_TYPE_KEYCLOAK_PARAM = "grant_type";
    public final static String CLIENT_CREDENTIALS_KEYCLOAK_CONST = "client_credentials";
    public final static String ACCES_TOKEN_KEYCLOAK_CONST = "access_token";
    public final static String ID_KEYCLOAK_CONST = "id";


    // Sites
    public final static String PROJECT_DASHBOARD_SITE = "project-dashboard";
    public final static String PROJECT_VIEW_SITE = "project-view";
    public final static String VOTUM_VIEW_SITE = "votum-view";
    public final static String CONFIGURATION_SITE = "config";
    public final static String NAVIGATION_BAR_SITE = "nav-bar";

    //Modules
    public final static String PROJECTS_MODULE = "PROJECTS";
    public final static String USER_MODULE = "USER";
    public final static String PROJECT_STATE_MODULE = "PROJECT_STATE";
    public final static String PROJECT_RESULTS_MODULE = "PROJECT_RESULTS";
    public final static String PROJECT_BRIDGEHEAD_MODULE = "PROJECT_BRIDGEHEAD";
    public final static String PROJECT_EDITION_MODULE = "PROJECT_EDITION";
    public final static String PROJECT_DOCUMENTS_MODULE = "PROJECT_DOCUMENTS";
    public final static String NOTIFICATIONS_MODULE = "NOTIFICATIONS";
    public final static String VOTUM_ACTIONS_MODULE = "VOTUM_ACTIONS";
    public final static String EXPORT_MODULE = "EXPORT";
    public final static String TOKEN_MANAGER_MODULE = "TOKEN_MANAGER";

    // Actions
    public final static String SET_DEVELOPER_USER_ACTION = "SET_DEVELOPER_USER";
    public final static String SET_PILOT_USER_ACTION = "SET_PILOT_USER";
    public final static String SET_FINAL_USER_ACTION = "SET_FINAL_USER";
    public final static String CREATE_PROJECT_ACTION = "CREATE_PROJECT";
    public final static String ACCEPT_PROJECT_ACTION = "ACCEPT_PROJECT";
    public final static String REJECT_PROJECT_ACTION = "REJECT_PROJECT";
    public final static String ARCHIVE_PROJECT_ACTION = "ARCHIVE_PROJECT";
    public final static String START_DEVELOP_STAGE_ACTION = "START_DEVELOP_STAGE";
    public final static String START_PILOT_STAGE_ACTION = "START_PILOT_STAGE";
    public final static String START_FINAL_STAGE_ACTION = "START_FINAL_STAGE";
    public final static String FINISH_PROJECT_ACTION = "FINISH_PROJECT";
    public final static String DOWNLOAD_APPLICATION_FORM_TEMPLATE_ACTION = "DOWNLOAD_APPLICATION_FORM_TEMPLATE";
    public final static String SAVE_QUERY_IN_BRIDGEHEAD_ACTION = "SAVE_QUERY_IN_BRIDGEHEAD";
    public final static String SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD_ACTION = "SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD";
    public final static String DOWNLOAD_AUTHENTICATION_SCRIPT_ACTION = "DOWNLOAD_AUTHENTICATION_SCRIPT";
    public final static String EDIT_PROJECT_ACTION = "EDIT_PROJECT";
    public final static String FETCH_EXPORTER_TEMPLATES_ACTION = "EXPORTER_TEMPLATES";
    public final static String FETCH_QUERY_FORMATS_ACTION = "FETCH_QUERY_FORMATS";
    public final static String FETCH_OUTPUT_FORMATS_ACTION = "FETCH_OUTPUT_FORMATS";
    public final static String UPLOAD_VOTUM_ACTION = "UPLOAD_VOTUM";
    public final static String UPLOAD_VOTUM_FOR_ALL_BRIDGEHEADS_ACTION = "UPLOAD_VOTUM_FOR_ALL_BRIDGEHEADS";
    public final static String UPLOAD_APPLICATION_FORM_ACTION = "UPLOAD_APPLICATION_FORM";
    public final static String UPLOAD_PUBLICATION_ACTION = "UPLOAD_PUBLICATION";
    public final static String UPLOAD_SCRIPT_ACTION = "UPLOAD_SCRIPT";
    public final static String UPLOAD_OTHER_DOCUMENT_ACTION = "UPLOAD_OTHER_DOCUMENT";
    public final static String ADD_PUBLICATION_URL_ACTION = "ADD_PUBLICATION_URL";
    public final static String ADD_OTHER_DOCUMENT_URL_ACTION = "ADD_OTHER_DOCUMENT_URL";
    public final static String DOWNLOAD_VOTUM_ACTION = "DOWNLOAD_VOTUM";
    public final static String DOWNLOAD_VOTUM_FOR_ALL_BRIDGEHEADS_ACTION = "DOWNLOAD_VOTUM_FOR_ALL_BRIDGEHEADS";
    public final static String FETCH_VOTUM_DESCRIPTION_ACTION = "FETCH_VOTUM_DESCRIPTION";
    public final static String FETCH_VOTUM_FOR_ALL_BRIDGEHEADS_DESCRIPTION_ACTION = "FETCH_VOTUM_FOR_ALL_BRIDGEHEADS_DESCRIPTION";
    public final static String EXISTS_VOTUM_ACTION = "EXISTS_VOTUM";
    public final static String EXISTS_VOTUM_FOR_ALL_BRIDGEHEADS_ACTION = "EXISTS_VOTUM_FOR_ALL_BRIDGEHEADS";
    public final static String DOWNLOAD_APPLICATION_FORM_ACTION = "DOWNLOAD_APPLICATION_FORM";
    public final static String FETCH_APPLICATION_FORM_DESCRIPTION_ACTION = "FETCH_APPLICATION_FORM_DESCRIPTION";
    public final static String EXISTS_APPLICATION_FORM_ACTION = "EXISTS_APPLICATION_FORM";
    public final static String EXISTS_SCRIPT_ACTION = "EXISTS_SCRIPT";
    public final static String DOWNLOAD_PUBLICATION_ACTION = "DOWNLOAD_PUBLICATION";
    public final static String DOWNLOAD_SCRIPT_ACTION = "DOWNLOAD_SCRIPT";
    public final static String FETCH_SCRIPT_DESCRIPTION_ACTION = "FETCH_SCRIPT_DESCRIPTION";
    public final static String DOWNLOAD_OTHER_DOCUMENT_ACTION = "DOWNLOAD_OTHER_DOCUMENT";
    public final static String ACCEPT_BRIDGEHEAD_PROJECT_ACTION = "ACCEPT_BRIDGEHEAD_PROJECT";
    public final static String REJECT_BRIDGEHEAD_PROJECT_ACTION = "REJECT_BRIDGEHEAD_PROJECT";
    public final static String ACCEPT_SCRIPT_ACTION = "ACCEPT_SCRIPT";
    public final static String REJECT_SCRIPT_ACTION = "REJECT_SCRIPT";
    public final static String REQUEST_SCRIPT_CHANGES_ACTION = "REQUEST_SCRIPT_CHANGES";
    public final static String FETCH_PROJECT_BRIDGEHEADS_ACTION = "FETCH_PROJECT_BRIDGEHEADS";
    public final static String FETCH_PROJECT_TYPES_ACTION = "FETCH_PROJECT_TYPES";
    public final static String FETCH_PROJECTS_ACTION = "FETCH_PROJECTS";
    public final static String FETCH_PUBLICATIONS_ACTION = "FETCH_PUBLICATIONS";
    public final static String FETCH_OTHER_DOCUMENTS_ACTION = "FETCH_OTHER_DOCUMENTS";
    public final static String ACCEPT_PROJECT_RESULTS_ACTION = "ACCEPT_PROJECT_RESULTS";
    public final static String REJECT_PROJECT_RESULTS_ACTION = "REJECT_PROJECT_RESULTS";
    public final static String ACCEPT_PROJECT_ANALYSIS_ACTION = "ACCEPT_PROJECT_ANALYSIS";
    public final static String REJECT_PROJECT_ANALYSIS_ACTION = "REJECT_PROJECT_ANALYSIS";
    public final static String REQUEST_CHANGES_IN_PROJECT_ANALYSIS_ACTION = "REQUEST_CHANGES_IN_PROJECT_ANALYSIS";
    public final static String REQUEST_CHANGES_IN_PROJECT_ACTION = "REQUEST_CHANGES_IN_PROJECT";
    public final static String FETCH_NOTIFICATIONS_ACTION = "FETCH_NOTIFICATIONS";
    public final static String SET_NOTIFICATION_AS_READ_ACTION = "SET_NOTIFICATION_AS_READ";
    public final static String FETCH_PROJECT_ACTION = "FETCH_PROJECT";
    public final static String FETCH_PROJECT_STATES_ACTION = "FETCH_PROJECT_STATES";
    public final static String FETCH_ALL_REGISTERED_BRIDGEHEADS_ACTION = "FETCH_ALL_REGISTERED_BRIDGEHEADS";
    public final static String FETCH_DATASHIELD_STATUS_ACTION = "FETCH_DATASHIELD_STATUS";
    public final static String FETCH_USERS_FOR_AUTOCOMPLETE_ACTION = "FETCH_USERS_FOR_AUTOCOMPLETE";
    public final static String FETCH_PROJECT_USERS_ACTION = "FETCH_PROJECT_USERS";
    public final static String FETCH_CURRENT_USER_ACTION = "FETCH_CURRENT_USER";
    public final static String EXIST_INVITED_USERS_ACTION = "EXIST_INVITED_USERS";
    public final static String FETCH_PROJECT_CONFIGURATIONS_ACTION = "FETCH_PROJECT_CONFIGURATIONS";
    public final static String EXISTS_AUTHENTICATION_SCRIPT_ACTION = "EXISTS_AUTHENTICATION_SCRIPT";
    public final static String FETCH_CURRENT_PROJECT_CONFIGURATION_ACTION = "FETCH_CURRENT_PROJECT_CONFIGURATION";
    public final static String SET_PROJECT_CONFIGURATION_ACTION = "SET_PROJECT_CONFIGURATION";
    public final static String FETCH_VISIBLE_PROJECT_BRIDGEHEADS_ACTION = "FETCH_VISIBLE_PROJECT_BRIDGEHEADS";
    public final static String FETCH_PROJECT_ROLES_ACTION = "FETCH_PROJECT_ROLES";
    public final static String SEND_EXPORT_FILES_TO_RESEARCH_ENVIRONMENT_ACTION = "SEND_EXPORT_FILES_TO_RESEARCH_ENVIRONMENT";
    public final static String ARE_EXPORT_FILES_TRANSFERRED_TO_RESEARCH_ENVIRONMENT_ACTION = "ARE_EXPORT_FILES_TRANSFERRED_TO_RESEARCH_ENVIRONMENT";
    public final static String ADD_USER_TO_MAILING_BLACK_LIST_ACTION = "ADD_USER_TO_MAILING_BLACK_LIST";
    public final static String REMOVE_USER_FROM_MAILING_BLACK_LIST_ACTION = "REMOVE_USER_FROM_MAILING_BLACK_LIST";
    public final static String FETCH_MAILING_BLACK_LIST_ACTION = "FETCH_MAILING_BLACK_LIST";
    public final static String FETCH_USERS_FOR_AUTOCOMPLETE_IN_MAILING_BLACK_LIST_ACTION = "FETCH_USERS_FOR_AUTOCOMPLETE_IN_MAILING_BLACK_LIST";
    public final static String ADD_PROJECT_BRIDGHEAD_RESULTS_URL_ACTION = "ADD_PROJECT_BRIDGHEAD_RESULTS_URL";
    public final static String ADD_PROJECT_RESULTS_URL_ACTION = "ADD_PROJECT_RESULTS_URL";
    public final static String ACCEPT_PROJECT_RESULTS_URL_ACTION = "ACCEPT_PROJECT_RESULTS_URL";
    public final static String REJECT_PROJECT_RESULTS_URL_ACTION = "REJECT_PROJECT_RESULTS_URL";
    public final static String REQUEST_CHANGES_IN_PROJECT_RESULTS_URL_ACTION = "REQUEST_CHANGES_IN_PROJECT_RESULTS_URL";
    public final static String ACCEPT_PROJECT_BRIDGEHEAD_RESULTS_URL_ACTION = "ACCEPT_PROJECT_BRIDGEHEAD_RESULTS_URL";
    public final static String REJECT_PROJECT_BRIDGEHEAD_RESULTS_URL_ACTION = "REJECT_PROJECT_BRIDGEHEAD_RESULTS_URL";
    public final static String REQUEST_CHANGES_IN_PROJECT_BRIDGEHEAD_RESULTS_URL_ACTION = "REQUEST_CHANGES_IN_PROJECT_BRIDGEHEAD_RESULTS_URL";
    public final static String FETCH_PROJECT_RESULTS_ACTION = "FETCH_PROJECT_RESULTS";
    public final static String FETCH_PROJECT_BRIDGEHEAD_RESULTS_ACTION = "FETCH_PROJECT_BRIDGEHEAD_RESULTS";
    public final static String FETCH_PROJECT_BRIDGEHEAD_RESULTS_FOR_OWN_BRIDGEHEAD_ACTION = "FETCH_PROJECT_BRIDGEHEAD_RESULTS_FOR_OWN_BRIDGEHEAD";
    public final static String FETCH_EMAIL_MESSAGE_AND_SUBJECT_ACTION = "FETCH_EMAIL_MESSAGE_AND_SUBJECT";
    public final static String IS_PROJECT_MANAGER_ADMIN_ACTION = "IS_PROJECT_MANAGER_ADMIN";
    public final static String FETCH_RESEARCH_ENVIRONMENT_URL_ACTION = "FETCH_RESEARCH_ENVIRONMENT_URL";
    public final static String EXISTS_RESEARCH_ENVIRONMENT_WORKSPACE_ACTION = "EXISTS_RESEARCH_ENVIRONMENT_WORKSPACE";


    // REST Services
    public final static String INFO = "/info";
    public final static String TEST = "/test";
    public final static String ACTIONS = "/actions";
    public final static String ALL_ACTIONS = "/all-actions";
    public final static String FETCH_PROJECTS = "/projects";
    public final static String SET_DEVELOPER_USER = "/set-developer-user";
    public final static String SET_PILOT_USER = "/set-pilot-user";
    public final static String SET_FINAL_USER = "/set-final-user";
    public final static String CREATE_QUERY_AND_DESIGN_PROJECT = "/create-query-and-design-project";
    public final static String EDIT_PROJECT = "/edit-project";
    public final static String FETCH_EXPORTER_TEMPLATES = "/exporter-templates";
    public final static String FETCH_QUERY_FORMATS = "/query-formats";
    public final static String FETCH_PROJECT_ROLES = "/project-roles";
    public final static String FETCH_OUTPUT_FORMATS = "/output-formats";
    public final static String FETCH_PROJECT_CONFIGURATIONS = "/project-configurations";
    public final static String FETCH_CURRENT_PROJECT_CONFIGURATION = "/project-configuration";
    public final static String SET_PROJECT_CONFIGURATION = "/project-configuration";
    public final static String DESIGN_PROJECT = "/design-project";
    public final static String CREATE_PROJECT = "/create-project";
    public final static String ACCEPT_PROJECT = "/accept-project";
    public final static String REJECT_PROJECT = "/reject-project";
    public final static String ADD_USER_TO_MAILING_BLACK_LIST = "/add-mailing-black-list";
    public final static String REMOVE_USER_FROM_MAILING_BLACK_LIST = "/remove-mailing-black-list";
    public final static String FETCH_MAILING_BLACK_LIST = "/mailing-black-list";
    public final static String FETCH_USERS_FOR_AUTOCOMPLETE_IN_MAILING_BLACK_LIST = "/mailing-black-list-autocomplete";
    public final static String ACCEPT_BRIDGEHEAD_PROJECT = "/accept-bridgehead-project";
    public final static String REJECT_BRIDGEHEAD_PROJECT = "/reject-bridgehead-project";
    public final static String ACCEPT_SCRIPT = "/accept-script";
    public final static String REJECT_SCRIPT = "/reject-script";
    public final static String REQUEST_SCRIPT_CHANGES = "/request-script-changes";
    public final static String ACCEPT_PROJECT_RESULTS = "/accept-project-results";
    public final static String REJECT_PROJECT_RESULTS = "/reject-project-results";
    public final static String REQUEST_CHANGES_IN_PROJECT = "/request-changes-in-project";
    public final static String ACCEPT_PROJECT_ANALYSIS = "/accept-project-analysis";
    public final static String REJECT_PROJECT_ANALYSIS = "/reject-project-analysis";
    public final static String REQUEST_CHANGES_IN_PROJECT_ANALYSIS = "/request-changes-in-project-analysis";
    public final static String FETCH_PROJECT_BRIDGEHEADS = "/project-bridgeheads";
    public final static String FETCH_VISIBLE_PROJECT_BRIDGEHEADS = "/visible-project-bridgeheads";
    public final static String FETCH_PROJECT = "/project";
    public final static String FETCH_PROJECT_STATES = "/project-states";
    public final static String ARCHIVE_PROJECT = "/archive-project";
    public final static String START_DEVELOP_STAGE = "/start-develop-project";
    public final static String START_PILOT_STAGE = "/start-pilot-project";
    public final static String START_FINAL_STAGE = "/start-final-project";
    public final static String FINISH_PROJECT = "/finish-project";
    public final static String CREATE_QUERY = "/create-query";
    public final static String FETCH_PROJECT_TYPES = "/project-types";
    public final static String UPLOAD_VOTUM = "/upload-votum";
    public final static String UPLOAD_VOTUM_FOR_ALL_BRIDGEHEADS = "/upload-votum-for-all-bridgeheads";
    public final static String UPLOAD_APPLICATION_FORM = "/upload-application-form";
    public final static String UPLOAD_PUBLICATION = "/upload-publication";
    public final static String UPLOAD_SCRIPT = "/upload-script";
    public final static String UPLOAD_OTHER_DOCUMENT = "/upload-other-document";
    public final static String ADD_PUBLICATION_URL = "/add-publication-url";
    public final static String ADD_OTHER_DOCUMENT_URL = "/add-other-document-url";
    public final static String DOWNLOAD_VOTUM = "/download-votum";
    public final static String DOWNLOAD_VOTUM_FOR_ALL_BRIDGEHEADS = "/download-votum-for-all-bridgeheads";
    public final static String FETCH_VOTUM_DESCRIPTION = "/votum-description";
    public final static String FETCH_VOTUM_FOR_ALL_BRIDGEHEADS_DESCRIPTION = "/votum-description-for-all-bridgeheads";
    public final static String EXISTS_VOTUM = "/exists-votum";
    public final static String EXISTS_VOTUM_FOR_ALL_BRIDGEHEADS = "/exists-votum-for-all-bridgeheads";
    public final static String DOWNLOAD_APPLICATION_FORM = "/download-application-form";
    public final static String FETCH_APPLICATION_FORM_DESCRIPTION = "/application-form-description";
    public final static String EXISTS_APPLICATION_FORM = "/exists-application-form";
    public final static String DOWNLOAD_PUBLICATION = "/download-publication";
    public final static String DOWNLOAD_SCRIPT = "/download-script";
    public final static String FETCH_SCRIPT_DESCRIPTION = "/script-description";
    public final static String EXISTS_SCRIPT = "/exists-script";
    public final static String DOWNLOAD_OTHER_DOCUMENT = "/download-other-document";
    public final static String DOWNLOAD_APPLICATION_FORM_TEMPLATE = "/download-application-form-template";
    public final static String SAVE_QUERY_IN_BRIDGEHEAD = "/save-query-in-bridgehead";
    public final static String SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD = "/save-and-execute-query-in-bridgehead";
    public final static String DOWNLOAD_AUTHENTICATION_SCRIPT = "/download-authentication-script";
    public final static String SEND_EXPORT_FILES_TO_RESEARCH_ENVIRONMENT = "/send-export-files-to-research-environment";
    public final static String ARE_EXPORT_FILES_TRANSFERRED_TO_RESEARCH_ENVIRONMENT = "/export-files-transferred-to-research-environment";
    public final static String EXISTS_AUTHENTICATION_SCRIPT = "/exists-authentication-script";
    public final static String FETCH_DATASHIELD_STATUS = "/datashield-status";
    public final static String FETCH_PUBLICATIONS = "/publications";
    public final static String FETCH_OTHER_DOCUMENTS = "/other-documents";
    public final static String FETCH_NOTIFICATIONS = "/notifications";
    public final static String SET_NOTIFICATION_AS_READ = "/read-notification";
    public final static String FETCH_ALL_REGISTERED_BRIDGEHEADS = "/bridgeheads";
    public final static String FETCH_USERS_FOR_AUTOCOMPLETE = "/autocomplete-users";
    public final static String FETCH_PROJECT_USERS = "/project-users";
    public final static String FETCH_CURRENT_USER = "/current-user";
    public final static String EXIST_INVITED_USERS = "/exist-invited-users";
    public final static String ADD_PROJECT_BRIDGHEAD_RESULTS_URL = "/project-bridgehead-results-url";
    public final static String ADD_PROJECT_RESULTS_URL = "/project-results-url";
    public final static String ACCEPT_PROJECT_RESULTS_URL = "/accept-project-results-url";
    public final static String REJECT_PROJECT_RESULTS_URL = "/reject-project-results-url";
    public final static String FETCH_PROJECT_RESULTS = "/project-results";
    public final static String REQUEST_CHANGES_IN_PROJECT_RESULTS_URL = "/request-changes-project-results-url";
    public final static String ACCEPT_PROJECT_BRIDGEHEAD_RESULTS_URL = "/accept-project-bridgehead-results-url";
    public final static String REJECT_PROJECT_BRIDGEHEAD_RESULTS_URL = "/reject-project-bridgehead-results-url";
    public final static String REQUEST_CHANGES_IN_PROJECT_BRIDGEHEAD_RESULTS_URL = "/request-changes-project-bridgehead-results-url";
    public final static String FETCH_PROJECT_BRIDGEHEAD_RESULTS = "/project-bridgehead-results";
    public final static String FETCH_PROJECT_BRIDGEHEAD_RESULTS_FOR_OWN_BRIDGEHEAD = "/own-project-bridgehead-results";
    public final static String FETCH_EMAIL_MESSAGE_AND_SUBJECT = "/email-message-and-subject";
    public final static String IS_PROJECT_MANAGER_ADMIN = "/is-project-manager-admin";
    public final static String FETCH_RESEARCH_ENVIRONMENT_URL = "/research-environment-url";
    public final static String EXISTS_RESEARCH_ENVIRONMENT_WORKSPACE = "/exists-research-environment-workspace";


    // REST Parameters
    public final static String PROJECT_CODE = "project-code";
    public final static String PROJECT_CONFIGURATION = "project-configuration";
    public final static String NOTIFICATION_ID = "notification-id";
    public final static String BRIDGEHEAD = "bridgehead";
    public final static String LANGUAGE = "language";
    public final static String MESSAGE = "message";
    public final static String BRIDGEHEADS = "bridgeheads";
    public final static String EXPLORER_IDS = "explorer-ids";
    public final static String PARTIAL_EMAIL = "partial-email";
    public final static String SITE = "site";
    public final static String EMAIL = "email";
    public final static String QUERY_FORMAT = "query-format";
    public final static String PROJECT_TYPE = "project-type";
    public final static String PROJECT_STATE = "project-state";
    public final static String ARCHIVED = "archived";
    public final static String PAGE = "page";
    public final static String PAGE_SIZE = "page-size";
    public final static String LAST_MODIFIED_DESC = "modified-desc";
    public final static String DOCUMENT = "document";
    public final static String DOCUMENT_URL = "document-url";
    public final static String FILENAME = "filename";
    public final static String QUERY_CODE = "query-code";
    public final static String LABEL = "label";
    public final static String DESCRIPTION = "description";
    public final static String OUTPUT_FORMAT = "output-format";
    public final static String TEMPLATE_ID = "template-id";
    public final static String HUMAN_READABLE = "human-readable";
    public final static String REDIRECT_EXPLORER_URL = "explorer-url";
    public final static String QUERY_CONTEXT = "query-context";
    public final static String RESULTS_URL = "results-url";
    public final static String PROJECT_ROLE = "project-role";
    public final static String EMAIL_TEMPLATE_TYPE = "email-template-type";

    public final static String EMAIL_CONTEXT_VARIABLE_TAG_ATTRIBUTE_DEFAULT_VALUE = "default";
    public final static String EMAIL_CONTEXT_VARIABLE_TAG_ATTRIBUTE_DEFAULT_VARIABLE = "default-var";

    // Application Properties
    public final static String JWKS_URI_PROPERTY = "spring.security.oauth2.client.provider.oidc.jwk-set-uri";
    public final static String REGISTERED_BRIDGEHEADS = "bridgeheads";
    public final static String FRONTEND_CONFIG = "frontend";
    public final static String HTTP_PROXY_PREFIX = "http.proxy";
    public final static String HTTPS_PROXY_PREFIX = "https.proxy";
    public final static String EMAIL_CONTEXT_PREFIX = "email";
    public final static String PRIMARY_MAIL_SENDER_PREFIX = "spring.mail.primary";
    public final static String TEST_MAIL_SENDER_PREFIX = "spring.mail.test";
    public final static String CODER_PREFIX = "coder";

    // Exporter
    public final static String SECURITY_ENABLED = "SECURITY_ENABLED";
    public final static String EXPORTER_PARAM_QUERY = "query";
    public final static String EXPORTER_PARAM_QUERY_FORMAT = "query-format";
    public final static String EXPORTER_PARAM_TEMPLATE_ID = "template-id";
    public final static String EXPORTER_PARAM_OUTPUT_FORMAT = "output-format";
    public static final String EXPORTER_PARAM_QUERY_EXPIRATION_DATE = "query-expiration-date";
    public final static String EXPORTER_PARAM_QUERY_CONTACT_ID = "query-contact-id";
    public static final String EXPORTER_PARAM_QUERY_LABEL = "query-label";
    public static final String EXPORTER_PARAM_QUERY_DESCRIPTION = "query-description";
    public static final String EXPORTER_PARAM_QUERY_EXECUTION_CONTACT_ID = "query-execution-contact-id";
    public static final String EXPORTER_PARAM_QUERY_CONTEXT = "query-context";
    public static final String EXPORTER_PARAM_DEFAULT_OUTPUT_FORMAT = "query-default-output-format";
    public static final String EXPORTER_PARAM_DEFAULT_TEMPLATE_ID = "query-default-template-id";
    public final static String EXPORTER_PARAM_QUERY_EXECUTION_ID = "query-execution-id";
    public static final String EXPORTER_QUERY_CONTEXT_PROJECT_ID = "PROJECT-ID";
    public final static String EXPORTER_QUERY_CONTEXT_SEPARATOR = ";";
    public final static String API_KEY = "ApiKey";
    public final static String EXPORTER_FETCH_QUERY_EXECUTION_URL_PATH = "/response?query-execution-id=";


    // Beam
    public final static String BEAM_FOCUS_METADATA_PROJECT = "exporter";
    public final static String BEAM_TASK_PATH = "/v1/tasks";
    public final static String BEAM_TASK_RESULTS_PATH = "/results";
    public final static String BEAM_TASK_WAIT_TIME_PARAM = "wait_time";
    public final static String BEAM_TASK_WAIT_COUNT_PARAM = "wait_count";

    // Token Manager Variables
    public final static String TOKEN_MANAGER_ROOT = "/api";
    public final static String TOKEN_MANAGER_TOKENS = "/token";
    public final static String TOKEN_MANAGER_TOKEN_STATUS = "/token-status";
    public final static String TOKEN_MANAGER_PROJECT = "/project";
    public final static String TOKEN_MANAGER_PROJECT_STATUS = "/project-status";
    public final static String TOKEN_MANAGER_SCRIPTS = "/script";
    public final static String TOKEN_MANAGER_REFRESH_TOKEN = "/refreshToken";
    public final static String AUTHENTICATION_SCRIPT_STATUS = "/authentication-status";
    public final static String AUTHENTICATION_SCRIPT_FILENAME_PREFIX = "authentication-script-";
    public final static String AUTHENTICATION_SCRIPT_FILENAME_SUFFIX = ".r";
    public final static String TOKEN_MANAGER_PARAMETER_BRIDGEHEAD = "bk";
    public final static String TOKEN_MANAGER_PARAMETER_PROJECT_CODE = "project_id";
    public final static String TOKEN_MANAGER_PARAMETER_EMAIL = "user_id";
    public final static String TOKEN_MANAGER_PARAMETER_PROJECT_STATUS = "project_status";
    public final static String TOKEN_MANAGER_PARAMETER_TOKEN_STATUS = "token_status";
    public final static String TOKEN_MANAGER_PARAMETER_TOKEN_CREATED_AT = "token_created_at";

    // Coder
    public final static String CODER_API_PATH = "/api/v2";
    public final static String CODER_SESSION_TOKEN_HEADER = "Coder-Session-Token";

    public final static String CODER_DELETE_TRANSITION = "delete";

    // Environment Variables
    public final static String PM_ADMIN_GROUPS = "PM_ADMIN_GROUPS";
    public final static String BK_USER_GROUP_PREFIX = "BK_USER_GROUP_PREFIX";
    public final static String BK_USER_GROUP_SUFFIX = "BK_USER_GROUP_SUFFIX";
    public final static String BK_ADMIN_GROUP_PREFIX = "BK_ADMIN_GROUP_PREFIX";
    public final static String BK_ADMIN_GROUP_SUFFIX = "BK_ADMIN_GROUP_SUFFIX";
    public final static String PROJECT_DOCUMENTS_DIRECTORY = "PROJECT_DOCUMENTS_DIRECTORY";
    public final static String PUBLIC_DOCUMENTS_DIRECTORY = "PUBLIC_DOCUMENTS_DIRECTORY";
    public final static String APPLICATION_FORM_FILENAME = "APPLICATION_FORM_FILENAME";
    public final static String PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT = "PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP";
    public final static String WEBCLIENT_BUFFER_SIZE_IN_BYTES = "WEBCLIENT_BUFFER_SIZE_IN_BYTES";
    public final static String WEBCLIENT_REQUEST_TIMEOUT_IN_SECONDS = "WEBCLIENT_REQUEST_TIMEOUT_IN_SECONDS";
    public final static String WEBCLIENT_CONNECTION_TIMEOUT_IN_SECONDS = "WEBCLIENT_CONNECTION_TIMEOUT_IN_SECONDS";
    public final static String WEBCLIENT_TCP_KEEP_IDLE_IN_SECONDS = "WEBCLIENT_TCP_KEEP_IDLE_IN_SECONDS";
    public final static String WEBCLIENT_TCP_KEEP_INTERVAL_IN_SECONDS = "WEBCLIENT_TCP_KEEP_INTERVAL_IN_SECONDS";
    public final static String WEBCLIENT_TCP_KEEP_CONNECTION_NUMBER_OF_TRIES = "WEBCLIENT_TCP_KEEP_CONNECTION_NUMBER_OF_TRIES";
    public final static String WEBCLIENT_MAX_NUMBER_OF_RETRIES = "WEBCLIENT_MAX_NUMBER_OF_RETRIES";
    public final static String WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE = "WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE";
    public final static String PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS = "PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS";
    public final static String PROJECT_MANAGER_EMAIL_FROM = "PROJECT_MANAGER_EMAIL_FROM";
    public final static String EMAIL_TEMPLATES_CONFIG_PATH = "EMAIL_TEMPLATES_CONFIG_PATH";
    public final static String ACTION_EXPLANATION_CONFIG_PATH = "ACTION_EXPLANATION_CONFIG_PATH";
    public final static String EMAIL_TEMPLATES_DIRECTORY = "EMAIL_TEMPLATES_DIRECTORY";
    public final static String EXPORT_TEMPLATES = "EXPORT_TEMPLATES";
    public final static String DATASHIELD_TEMPLATES = "DATASHIELD_TEMPLATES";
    public final static String RESEARCH_ENVIRONMENT_TEMPLATES = "RESEARCH_ENVIRONMENT_TEMPLATES";
    public final static String BEAM_PROJECT_MANAGER_ID = "BEAM_PROJECT_MANAGER_ID";
    public final static String BEAM_TTL = "BEAM_TTL";
    public final static String BEAM_FAILURE_STRATEGY_BACKOFF_IN_MILLISECONDS = "BEAM_FAILURE_STRATEGY_BACKOFF_IN_MILLISECONDS";
    public final static String BEAM_FAILURE_STRATEGY_MAX_TRIES = "BEAM_FAILURE_STRATEGY_MAX_TRIES";
    public final static String BEAM_URL = "BEAM_URL";
    public final static String BEAM_API_KEY = "BEAM_API_KEY";
    public final static String TOKEN_MANAGER_URL = "TOKEN_MANAGER_URL";
    public final static String ENABLE_EMAILS = "ENABLE_EMAILS";
    public final static String MANAGE_TOKENS_CRON_EXPRESSION = "MANAGE_TOKENS_CRON_EXPRESSION";
    public final static String EXPORTER_CRON_EXPRESSION = "EXPORTER_CRON_EXPRESSION";
    public final static String CHECK_EXPIRED_ACTIVE_PROJECTS_CRON_EXPRESSION = "CHECK_EXPIRED_ACTIVE_PROJECTS_CRON_EXPRESSION";
    public final static String EXPLORER_URL = "EXPLORER_URL";
    public final static String ENABLE_TOKEN_MANAGER = "ENABLE_TOKEN_MANAGER";
    public final static String EXPLORER_REDIRECT_URI_PARAMETER = "EXPLORER_REDIRECT_URI_PARAMETER";
    public final static String FRONTEND_PROJECT_CONFIG_PATH = "FRONTEND_PROJECT_CONFIG_PATH";
    public final static String RSTUDIO_GROUP_IMPLEMENTATION = "RSTUDIO_GROUP_IMPLEMENTATION";
    public final static String KEYCLOAK_RSTUDIO_GROUP_CLIENT_ID = "KEYCLOAK_RSTUDIO_GROUP_CLIENT_ID";
    public final static String KEYCLOAK_RSTUDIO_GROUP_CLIENT_SECRET = "KEYCLOAK_RSTUDIO_GROUP_CLIENT_SECRET";
    public final static String KEYCLOAK_RSTUDIO_GROUP = "KEYCLOAK_RSTUDIO_GROUP";
    public final static String ENABLE_RSTUDIO_GROUP_MANAGER = "ENABLE_RSTUDIO_GROUP_MANAGER";
    public final static String OIDC_URL = "OIDC_URL";
    public final static String OIDC_REALM = "OIDC_REALM";
    public final static String ENABLE_EXPORTER = "ENABLE_EXPORTER";
    public final static String MAX_TIME_TO_WAIT_FOCUS_TASK_IN_MINUTES = "MAX_TIME_TO_WAIT_FOCUS_TASK_IN_MINUTES";
    public final static String DEFAULT_LANGUAGE = "DEFAULT_LANGUAGE";

    public final static String JWT_GROUPS_CLAIM = "JWT_GROUPS_CLAIM";
    public final static String JWT_EMAIL_CLAIM = "JWT_EMAIL_CLAIM";
    public final static String JWT_FIRST_NAME_CLAIM = "JWT_FIRST_NAME_CLAIM";
    public final static String JWT_LAST_NAME_CLAIM = "JWT_LAST_NAME_CLAIM";

    public final static String CODER_BASE_URL = "CODER_BASE_URL";
    public final static String CODER_ORGANISATION_ID = "CODER_ORGANISATION_ID";
    public final static String CODER_MEMBER_ID = "CODER_MEMBER_ID"; // Used in coder http request paths
    public final static String CODER_WORKSPACE_ID = "CODER_WORKSPACE_ID";
    public final static String CODER_CREATE_PATH = "CODER_CREATE_PATH";
    public final static String CODER_DELETE_PATH = "CODER_DELETE_PATH";
    public final static String CODER_SESSION_TOKEN = "CODER_SESSION_TOKEN";
    public final static String CODER_CRON_EXPRESSION = "CODER_CRON_EXPRESSION";
    public final static String CODER_WORKSPACE_NAME_MAX_LENGTH = "CODER_WORKSPACE_NAME_MAX_LENGTH";

    public final static String CODER_BEAM_ID_SUFFIX = "CODER_BEAM_ID_SUFFIX";
    public final static String CODER_TEST_FILE_BEAM_ID = "CODER_TEST_FILE_BEAM_ID";
    public final static String ENABLE_CODER = "ENABLE_CODER";

    public final static String EMAIL_SENDER_CORE_POOL_SIZE = "EMAIL_SENDER_CORE_POOL_SIZE";
    public final static String EMAIL_SENDER_MAX_POOL_SIZE = "EMAIL_SENDER_MAX_POOL_SIZE";
    public final static String EMAIL_SENDER_QUEUE_CAPACITY = "EMAIL_SENDER_QUEUE_CAPACITY";

    public final static String NOTIFICATION_CORE_POOL_SIZE = "NOTIFICATION_CORE_POOL_SIZE";
    public final static String NOTIFICATION_MAX_POOL_SIZE = "NOTIFICATION_MAX_POOL_SIZE";
    public final static String NOTIFICATION_QUEUE_CAPACITY = "NOTIFICATION_QUEUE_CAPACITY";

    public final static String EXPORTER_CORE_POOL_SIZE = "EXPORTER_CORE_POOL_SIZE";
    public final static String EXPORTER_MAX_POOL_SIZE = "EXPORTER_MAX_POOL_SIZE";
    public final static String EXPORTER_QUEUE_CAPACITY = "EXPORTER_QUEUE_CAPACITY";
    public final static String TEST_EMAIL_DOMAINS = "TEST_EMAIL_DOMAINS";

    public final static String DB_ENCRYPTION_PRIVATE_KEY_IN_BASE64 = "DB_ENCRYPTION_PRIVATE_KEY_IN_BASE64";
    public final static String DB_ENCRYPTION_ALGORITHM = "DB_ENCRYPTION_ALGORITHM";

    public final static String APP_REGISTER_BASE_URL = "APP_REGISTER_BASE_URL";
    public final static String APP_REGISTER_API_KEY = "APP_REGISTER_API_KEY";
    public final static String APP_REGISTER_AUTHORIZATION_FORMAT = "APP_REGISTER_AUTHORIZATION_FORMAT";
    public final static String ENABLE_APP_REGISTER = "ENABLE_APP_REGISTER";

    // Spring Values (SV)
    public final static String HEAD_SV = "${";
    public final static String BOTTOM_SV = "}";
    public final static String PM_ADMIN_GROUPS_SV = HEAD_SV + PM_ADMIN_GROUPS + BOTTOM_SV;
    public final static String JWT_GROUPS_CLAIM_SV = HEAD_SV + JWT_GROUPS_CLAIM + ":groups" + BOTTOM_SV;
    public final static String JWT_EMAIL_CLAIM_SV = HEAD_SV + JWT_EMAIL_CLAIM + ":email" + BOTTOM_SV;
    public final static String JWT_FIRST_NAME_CLAIM_SV = HEAD_SV + JWT_FIRST_NAME_CLAIM + ":given_name" + BOTTOM_SV;
    public final static String JWT_LAST_NAME_CLAIM_SV = HEAD_SV + JWT_LAST_NAME_CLAIM + ":family_name" + BOTTOM_SV;
    public final static String JWKS_URI_PROPERTY_SV = HEAD_SV + JWKS_URI_PROPERTY + BOTTOM_SV;
    public final static String BK_USER_GROUP_PREFIX_SV = HEAD_SV + BK_USER_GROUP_PREFIX + BOTTOM_SV;
    public final static String BK_USER_GROUP_SUFFIX_SV = HEAD_SV + BK_USER_GROUP_SUFFIX + BOTTOM_SV;
    public final static String BK_ADMIN_GROUP_PREFIX_SV = HEAD_SV + BK_ADMIN_GROUP_PREFIX + BOTTOM_SV;
    public final static String BK_ADMIN_GROUP_SUFFIX_SV = HEAD_SV + BK_ADMIN_GROUP_SUFFIX + BOTTOM_SV;
    public final static String PROJECT_DOCUMENTS_DIRECTORY_SV = HEAD_SV + PROJECT_DOCUMENTS_DIRECTORY + BOTTOM_SV;
    public final static String PUBLIC_DOCUMENTS_DIRECTORY_SV = HEAD_SV + PUBLIC_DOCUMENTS_DIRECTORY + BOTTOM_SV;
    public final static String APPLICATION_FORM_FILENAME_SV = HEAD_SV + APPLICATION_FORM_FILENAME + BOTTOM_SV;
    public final static String PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT_SV = HEAD_SV + PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT + ":yyyyMMdd-HHmmss" + BOTTOM_SV;
    public final static String SECURITY_ENABLED_SV = HEAD_SV + SECURITY_ENABLED + ":true" + BOTTOM_SV;
    public final static String WEBCLIENT_BUFFER_SIZE_IN_BYTES_SV =
            HEAD_SV + WEBCLIENT_BUFFER_SIZE_IN_BYTES + ":#{36 * 1024 * 1024}" + BOTTOM_SV;
    public final static String WEBCLIENT_REQUEST_TIMEOUT_IN_SECONDS_SV =
            HEAD_SV + WEBCLIENT_REQUEST_TIMEOUT_IN_SECONDS + ":180" + BOTTOM_SV;
    public final static String WEBCLIENT_CONNECTION_TIMEOUT_IN_SECONDS_SV =
            HEAD_SV + WEBCLIENT_CONNECTION_TIMEOUT_IN_SECONDS + ":180" + BOTTOM_SV;
    public final static String WEBCLIENT_TCP_KEEP_IDLE_IN_SECONDS_SV =
            HEAD_SV + WEBCLIENT_TCP_KEEP_IDLE_IN_SECONDS + ":300" + BOTTOM_SV;
    public final static String WEBCLIENT_TCP_KEEP_INTERVAL_IN_SECONDS_SV =
            HEAD_SV + WEBCLIENT_TCP_KEEP_INTERVAL_IN_SECONDS + ":60" + BOTTOM_SV;
    public final static String WEBCLIENT_TCP_KEEP_CONNECTION_NUMBER_OF_TRIES_SV =
            HEAD_SV + WEBCLIENT_TCP_KEEP_CONNECTION_NUMBER_OF_TRIES + ":10" + BOTTOM_SV;
    public final static String MAX_TIME_TO_WAIT_FOCUS_TASK_IN_MINUTES_SV =
            HEAD_SV + MAX_TIME_TO_WAIT_FOCUS_TASK_IN_MINUTES + ":5" + BOTTOM_SV;
    public final static String WEBCLIENT_MAX_NUMBER_OF_RETRIES_SV =
            HEAD_SV + WEBCLIENT_MAX_NUMBER_OF_RETRIES + ":3" + BOTTOM_SV;
    public final static String WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE_SV =
            HEAD_SV + WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE + ":5" + BOTTOM_SV;
    public final static String PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS_SV =
            HEAD_SV + PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS + ":90" + BOTTOM_SV;
    public final static String PROJECT_MANAGER_EMAIL_FROM_SV =
            HEAD_SV + PROJECT_MANAGER_EMAIL_FROM + ":no-reply@project-manager.com" + BOTTOM_SV;
    public final static String EMAIL_TEMPLATES_CONFIG_PATH_SV = HEAD_SV + EMAIL_TEMPLATES_CONFIG_PATH + BOTTOM_SV;
    public final static String ACTION_EXPLANATION_CONFIG_PATH_SV = HEAD_SV + ACTION_EXPLANATION_CONFIG_PATH + BOTTOM_SV;
    public final static String EMAIL_TEMPLATES_DIRECTORY_SV = HEAD_SV + EMAIL_TEMPLATES_DIRECTORY + BOTTOM_SV;
    public final static String EXPORT_TEMPLATES_SV = HEAD_SV + EXPORT_TEMPLATES + BOTTOM_SV;
    public final static String DATASHIELD_TEMPLATES_SV = HEAD_SV + DATASHIELD_TEMPLATES + BOTTOM_SV;
    public final static String RESEARCH_ENVIRONMENT_TEMPLATES_SV = HEAD_SV + RESEARCH_ENVIRONMENT_TEMPLATES + BOTTOM_SV;
    public final static String TOKEN_MANAGER_URL_SV = HEAD_SV + TOKEN_MANAGER_URL + BOTTOM_SV;
    public final static String BEAM_PROJECT_MANAGER_ID_SV = HEAD_SV + BEAM_PROJECT_MANAGER_ID + BOTTOM_SV;
    public final static String BEAM_TTL_SV = HEAD_SV + BEAM_TTL + ":180s" + BOTTOM_SV;
    public final static String BEAM_FAILURE_STRATEGY_BACKOFF_IN_MILLISECONDS_SV =
            HEAD_SV + BEAM_FAILURE_STRATEGY_BACKOFF_IN_MILLISECONDS + ":1000" + BOTTOM_SV;
    public final static String BEAM_FAILURE_STRATEGY_MAX_TRIES_SV = HEAD_SV + BEAM_FAILURE_STRATEGY_MAX_TRIES + ":5" + BOTTOM_SV;
    public final static String BEAM_URL_SV = HEAD_SV + BEAM_URL + BOTTOM_SV;
    public final static String BEAM_API_KEY_SV = HEAD_SV + BEAM_API_KEY + BOTTOM_SV;
    public final static String ENABLE_EMAILS_SV = HEAD_SV + ENABLE_EMAILS + ":true" + BOTTOM_SV;
    public final static String ENABLE_TOKEN_MANAGER_SV = HEAD_SV + ENABLE_TOKEN_MANAGER + ":true" + BOTTOM_SV;
    public final static String ENABLE_EXPORTER_SV = HEAD_SV + ENABLE_EXPORTER + ":true" + BOTTOM_SV;
    public final static String MANAGE_TOKENS_CRON_EXPRESSION_SV =
            HEAD_SV + MANAGE_TOKENS_CRON_EXPRESSION + ":#{'0 * * * * *'}" + BOTTOM_SV;
    public final static String EXPORTER_CRON_EXPRESSION_SV =
            HEAD_SV + EXPORTER_CRON_EXPRESSION + ":#{'45 * * * * *'}" + BOTTOM_SV;
    public final static String CHECK_EXPIRED_ACTIVE_PROJECTS_CRON_EXPRESSION_SV =
            HEAD_SV + CHECK_EXPIRED_ACTIVE_PROJECTS_CRON_EXPRESSION + ":#{'30 * * * * *'}" + BOTTOM_SV;
    public final static String EXPLORER_URL_SV = HEAD_SV + EXPLORER_URL + BOTTOM_SV;
    public final static String EXPLORER_REDIRECT_URI_PARAMETER_SV = HEAD_SV + EXPLORER_REDIRECT_URI_PARAMETER + BOTTOM_SV;
    public final static String FRONTEND_PROJECT_CONFIG_PATH_SV = HEAD_SV + FRONTEND_PROJECT_CONFIG_PATH + BOTTOM_SV;
    public final static String RSTUDIO_GROUP_IMPLEMENTATION_SV = HEAD_SV + RSTUDIO_GROUP_IMPLEMENTATION + ":" + RSTUDIO_GROUP_KEYCLOAK_IMPLEMENTATION + BOTTOM_SV;
    public final static String KEYCLOAK_RSTUDIO_GROUP_CLIENT_ID_SV = HEAD_SV + KEYCLOAK_RSTUDIO_GROUP_CLIENT_ID + BOTTOM_SV;
    public final static String KEYCLOAK_RSTUDIO_GROUP_CLIENT_SECRET_SV = HEAD_SV + KEYCLOAK_RSTUDIO_GROUP_CLIENT_SECRET + BOTTOM_SV;
    public final static String KEYCLOAK_RSTUDIO_GROUP_SV = HEAD_SV + KEYCLOAK_RSTUDIO_GROUP + BOTTOM_SV;
    public final static String ENABLE_RSTUDIO_GROUP_MANAGER_SV = HEAD_SV + ENABLE_RSTUDIO_GROUP_MANAGER + ":true" + BOTTOM_SV;
    public final static String OIDC_URL_SV = HEAD_SV + OIDC_URL + BOTTOM_SV;
    public final static String OIDC_REALM_SV = HEAD_SV + OIDC_REALM + BOTTOM_SV;
    public final static String CODER_BASE_URL_SV = HEAD_SV + CODER_BASE_URL + BOTTOM_SV;
    public final static String CODER_ORGANISATION_ID_SV = HEAD_SV + CODER_ORGANISATION_ID + BOTTOM_SV;
    public final static String CODER_MEMBER_ID_SV = HEAD_SV + CODER_MEMBER_ID + BOTTOM_SV;
    public final static String CODER_CREATE_PATH_SV = HEAD_SV + CODER_CREATE_PATH + BOTTOM_SV;
    public final static String CODER_DELETE_PATH_SV = HEAD_SV + CODER_DELETE_PATH + BOTTOM_SV;
    public final static String CODER_SESSION_TOKEN_SV = HEAD_SV + CODER_SESSION_TOKEN + BOTTOM_SV;
    public final static String CODER_BEAM_ID_SUFFIX_SV = HEAD_SV + CODER_BEAM_ID_SUFFIX + BOTTOM_SV;
    public final static String CODER_TEST_FILE_BEAM_ID_SV = HEAD_SV + CODER_TEST_FILE_BEAM_ID + BOTTOM_SV;
    public final static String CODER_CRON_EXPRESSION_SV = HEAD_SV + CODER_CRON_EXPRESSION + BOTTOM_SV;
    public final static String CODER_WORKSPACE_NAME_MAX_LENGTH_SV = HEAD_SV + CODER_WORKSPACE_NAME_MAX_LENGTH + ":32" + BOTTOM_SV;


    public final static String ENABLE_CODER_SV = HEAD_SV + ENABLE_CODER + ":true" + BOTTOM_SV;

    public final static String EMAIL_SENDER_CORE_POOL_SIZE_SV = HEAD_SV + EMAIL_SENDER_CORE_POOL_SIZE + ":4" + BOTTOM_SV;
    public final static String EMAIL_SENDER_MAX_POOL_SIZE_SV = HEAD_SV + EMAIL_SENDER_MAX_POOL_SIZE + ":8" + BOTTOM_SV;
    public final static String EMAIL_SENDER_QUEUE_CAPACITY_SV = HEAD_SV + EMAIL_SENDER_QUEUE_CAPACITY + ":500" + BOTTOM_SV;

    public final static String NOTIFICATION_CORE_POOL_SIZE_SV = HEAD_SV + NOTIFICATION_CORE_POOL_SIZE + ":4" + BOTTOM_SV;
    public final static String NOTIFICATION_MAX_POOL_SIZE_SV = HEAD_SV + NOTIFICATION_MAX_POOL_SIZE + ":8" + BOTTOM_SV;
    public final static String NOTIFICATION_QUEUE_CAPACITY_SV = HEAD_SV + NOTIFICATION_QUEUE_CAPACITY + ":500" + BOTTOM_SV;

    public final static String EXPORTER_CORE_POOL_SIZE_SV = HEAD_SV + EXPORTER_CORE_POOL_SIZE + ":4" + BOTTOM_SV;
    public final static String EXPORTER_MAX_POOL_SIZE_SV = HEAD_SV + EXPORTER_MAX_POOL_SIZE + ":8" + BOTTOM_SV;
    public final static String EXPORTER_QUEUE_CAPACITY_SV = HEAD_SV + EXPORTER_QUEUE_CAPACITY + ":500" + BOTTOM_SV;
    public final static String DEFAULT_LANGUAGE_SV = HEAD_SV + DEFAULT_LANGUAGE + ":EN" + BOTTOM_SV;

    public final static String TEST_EMAIL_DOMAINS_SV = HEAD_SV + TEST_EMAIL_DOMAINS + ":" + BOTTOM_SV;

    public final static String DB_ENCRYPTION_PRIVATE_KEY_IN_BASE64_SV = HEAD_SV + DB_ENCRYPTION_PRIVATE_KEY_IN_BASE64 + BOTTOM_SV;
    public final static String DB_ENCRYPTION_ALGORITHM_SV = HEAD_SV + DB_ENCRYPTION_ALGORITHM + ":AES" + BOTTOM_SV;

    public final static String APP_REGISTER_BASE_URL_SV = HEAD_SV + APP_REGISTER_BASE_URL + BOTTOM_SV;
    public final static String APP_REGISTER_API_KEY_SV = HEAD_SV + APP_REGISTER_API_KEY + BOTTOM_SV;
    public final static String APP_REGISTER_AUTHORIZATION_FORMAT_SV = HEAD_SV + APP_REGISTER_AUTHORIZATION_FORMAT + ":'ApiKey {}'" + BOTTOM_SV;
    public final static String ENABLE_APP_REGISTER_SV = HEAD_SV + ENABLE_APP_REGISTER + ":true" + BOTTOM_SV;

    // Async Configuration
    public final static String ASYNC_EMAIL_SENDER_EXECUTOR = "email-sender";
    public final static String ASYNC_NOTIFICATION_EXECUTOR = "notification";
    public final static String ASYNC_EXPORTER_EXECUTOR = "exporter";

    // Thymeleaf
    public final static int THYMELEAF_PROCESSOR_PRECEDENCE = 1000;
    public final static int THYMELEAF_DIALECT_PRECEDENCE = 1000;
    public final static String THYMELEAF_DIALECT_NAME = "Project Manager";
    public final static String THYMELEAF_DIALECT_PREFIX = "pm";

    // Variable name placeholders
    public final static String HYPHEN = "minus";
    public final static String UNDERSCORE = "underscore";

    // App Register
    public final static String REGISTER_PATH = "/beam-app";
    public final static String UNREGISTER_PATH = "/beam-app";


    // Others
    public final static String TEST_EMAIL = "test@project-manager.com";
    public final static String TEST_BRIDGEHEAD = "bridgehead-test";
    public final static int RANDOM_FILENAME_SIZE = 20;
    public final static int PROJECT_CODE_SIZE = 20;
    public final static int QUERY_CODE_SIZE = 20;
    public final static String NO_BRIDGEHEAD = "NONE";
    public final static String THIS_IS_A_TEST = "This is a test";
    public final static String CUSTOM_PROJECT_CONFIGURATION = "CUSTOM";
    public final static String EMAIL_SERVICE = "EMAIL_SERVICE";
    public final static String BASE_64 = "b64";
    public final static String HTTP_PROTOCOL_SCHEMA = "http";
    public final static String HTTPS_PROTOCOL_SCHEMA = "https";
    public final static String PRIMARY_MAIL_SENDER = "primaryJavaMailSender";
    public final static String TEST_MAIL_SENDER = "testJavaMailSender";
    public final static String NOT_AUTHORIZED = "Not authorized yet";


}
