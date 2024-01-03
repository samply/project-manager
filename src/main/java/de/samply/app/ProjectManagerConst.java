package de.samply.app;

public class ProjectManagerConst {

    public final static String APP_NAME = "Project Manager";

    // Sites
    public final static String PROJECT_DASHBOARD_SITE = "project-dashboard";
    public final static String PROJECT_VIEW_SITE = "project-view";

    //Modules
    public final static String USER_MODULE = "USER";
    public final static String PROJECT_STATE_MODULE = "PROJECT_STATE";
    public final static String PROJECT_EDITION_MODULE = "PROJECT_EDITION";
    public final static String PROJECT_DOCUMENTS_MODULE = "PROJECT_DOCUMENTS";
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
    public final static String UPLOAD_PROJECT_DOCUMENT_ACTION = "UPLOAD_PROJECT_DOCUMENT";
    public final static String ADD_PROJECT_DOCUMENT_URL_ACTION = "ADD_PROJECT_DOCUMENT_URL";
    public final static String DOWNLOAD_PROJECT_DOCUMENT_ACTION = "DOWNLOAD_PROJECT_DOCUMENT";
    public final static String DOWNLOAD_APPLICATION_FORM_ACTION = "DOWNLOAD_APPLICATION_FORM";
    public final static String SAVE_QUERY_IN_BRIDGEHEAD_ACTION = "SAVE_QUERY_IN_BRIDGEHEAD";
    public final static String SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD_ACTION = "SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD";
    public final static String FETCH_AUTHENTICATION_SCRIPT_ACTION = "FETCH_AUTHENTICATION_SCRIPT";
    public final static String EDIT_PROJECT_ACTION = "EDIT_PROJECT";
    public final static String FETCH_EXPORTER_TEMPLATES_ACTION = "EXPORTER_TEMPLATES";
    public final static String FETCH_QUERY_FORMATS_ACTION = "FETCH_QUERY_FORMATS";
    public final static String FETCH_OUTPUT_FORMATS_ACTION = "FETCH_OUTPUT_FORMATS";


    // REST Services
    public final static String INFO = "/info";
    public final static String TEST = "/test";
    public final static String ACTIONS = "/actions";
    public final static String SET_DEVELOPER_USER = "/set-developer-user";
    public final static String SET_PILOT_USER = "/set-pilot-user";
    public final static String SET_FINAL_USER = "/set-final-user";
    public final static String CREATE_QUERY_AND_DESIGN_PROJECT = "/create-query-and-design-project";
    public final static String EDIT_PROJECT = "/edit-project";
    public final static String FETCH_EXPORTER_TEMPLATES = "/exporter-templates";
    public final static String FETCH_QUERY_FORMATS = "/query-formats";
    public final static String FETCH_OUTPUT_FORMATS = "/output-formats";
    public final static String DESIGN_PROJECT = "/design-project";
    public final static String CREATE_PROJECT = "/create-project";
    public final static String ACCEPT_PROJECT = "/accept-project";
    public final static String REJECT_PROJECT = "/reject-project";
    public final static String ARCHIVE_PROJECT = "/archive-project";
    public final static String START_DEVELOP_STAGE = "/start-develop-project";
    public final static String START_PILOT_STAGE = "/start-pilot-project";
    public final static String START_FINAL_STAGE = "/start-final-project";
    public final static String FINISH_PROJECT = "/finish-project";
    public final static String CREATE_QUERY = "/create-query";
    public final static String UPLOAD_PROJECT_DOCUMENT = "/upload-project-document";
    public final static String ADD_PROJECT_DOCUMENT_URL = "/add-project-document-url";
    public final static String DOWNLOAD_PROJECT_DOCUMENT = "/download-project-document";
    public final static String DOWNLOAD_APPLICATION_FORM = "/download-application-form";
    public final static String SAVE_QUERY_IN_BRIDGEHEAD = "/save-query-in-bridgehead";
    public final static String SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD = "/save-and-execute-query-in-bridgehead";
    public final static String FETCH_AUTHENTICATION_SCRIPT = "/authentication-script";

    // REST Parameters
    public final static String PROJECT_CODE = "project-code";
    public final static String BRIDGEHEAD = "bridgehead";
    public final static String BRIDGEHEADS = "bridgeheads";
    public final static String SITE = "site";
    public final static String EMAIL = "email";
    public final static String QUERY_FORMAT = "query-format";
    public final static String PROJECT_TYPE = "project-type";
    public final static String DOCUMENT = "document";
    public final static String DOCUMENT_TYPE = "document-type";
    public final static String DOCUMENT_URL = "document-url";
    public final static String FILENAME = "filename";
    public final static String QUERY_CODE = "query-code";
    public final static String LABEL = "label";
    public final static String DESCRIPTION = "description";
    public final static String OUTPUT_FORMAT = "output-format";
    public final static String TEMPLATE_ID = "template-id";
    public final static String HUMAN_READABLE = "human-readable";
    public final static String EXPLORER_URL = "explorer-url";
    public final static String QUERY_CONTEXT = "query-context";

    // Email context properties
    public final static String EMAIL_CONTEXT_BRIDGEHEAD = "bridgehead";


    // Application Properties
    public final static String JWT_GROUPS_CLAIM_PROPERTY = "jwt.groups.claim";
    public final static String REGISTERED_BRIDGEHEADS = "bridgeheads";
    public final static String FRONTEND_CONFIG = "frontend";

    // Exporter Variables
    public final static String SECURITY_ENABLED = "SECURITY_ENABLED";
    public final static String IS_TEST_ENVIRONMENT = "IS_TEST_ENVIRONMENT";
    public final static String EXPORTER_REQUEST = "/request";
    public final static String EXPORTER_CREATE_QUERY = "/create-query";
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
    public static final String EXPORTER_QUERY_CONTEXT_PROJECT_ID = "PROJECT-ID";
    public final static String HTTP_HEADER_API_KEY = "x-api-key";


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
    public final static String EMAIL_TEMPLATES_CONFIG = "EMAIL_TEMPLATES_CONFIG";
    public final static String EMAIL_TEMPLATES_DIRECTORY = "EMAIL_TEMPLATES_DIRECTORY";
    public final static String EXPORT_TEMPLATES = "EXPORT_TEMPLATES";
    public final static String DATASHIELD_TEMPLATES = "DATASHIELD_TEMPLATES";


    // Spring Values (SV)
    public final static String HEAD_SV = "${";
    public final static String BOTTOM_SV = "}";
    public final static String PM_ADMIN_GROUPS_SV = HEAD_SV + PM_ADMIN_GROUPS + BOTTOM_SV;
    public final static String JWT_GROUPS_CLAIM_PROPERTY_SV = HEAD_SV + JWT_GROUPS_CLAIM_PROPERTY + BOTTOM_SV;
    public final static String BK_USER_GROUP_PREFIX_SV = HEAD_SV + BK_USER_GROUP_PREFIX + BOTTOM_SV;
    public final static String BK_USER_GROUP_SUFFIX_SV = HEAD_SV + BK_USER_GROUP_SUFFIX + BOTTOM_SV;
    public final static String BK_ADMIN_GROUP_PREFIX_SV = HEAD_SV + BK_ADMIN_GROUP_PREFIX + BOTTOM_SV;
    public final static String BK_ADMIN_GROUP_SUFFIX_SV = HEAD_SV + BK_ADMIN_GROUP_SUFFIX + BOTTOM_SV;
    public final static String PROJECT_DOCUMENTS_DIRECTORY_SV = HEAD_SV + PROJECT_DOCUMENTS_DIRECTORY + BOTTOM_SV;
    public final static String PUBLIC_DOCUMENTS_DIRECTORY_SV = HEAD_SV + PUBLIC_DOCUMENTS_DIRECTORY + BOTTOM_SV;
    public final static String APPLICATION_FORM_FILENAME_SV = HEAD_SV + APPLICATION_FORM_FILENAME + BOTTOM_SV;
    public final static String PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT_SV = HEAD_SV + PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT + ":yyyyMMdd-HHmmss" + BOTTOM_SV;
    public final static String SECURITY_ENABLED_SV = HEAD_SV + SECURITY_ENABLED + ":true" + BOTTOM_SV;
    public final static String IS_TEST_ENVIRONMENT_SV = HEAD_SV + IS_TEST_ENVIRONMENT + ":false" + BOTTOM_SV;
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
    public final static String WEBCLIENT_MAX_NUMBER_OF_RETRIES_SV =
            HEAD_SV + WEBCLIENT_MAX_NUMBER_OF_RETRIES + ":3" + BOTTOM_SV;
    public final static String WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE_SV =
            HEAD_SV + WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE + ":5" + BOTTOM_SV;
    public final static String PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS_SV =
            HEAD_SV + PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS + ":90" + BOTTOM_SV;
    public final static String PROJECT_MANAGER_EMAIL_FROM_SV =
            HEAD_SV + PROJECT_MANAGER_EMAIL_FROM + ":no-reply@project-manager.com" + BOTTOM_SV;
    public final static String EMAIL_TEMPLATES_CONFIG_SV = HEAD_SV + EMAIL_TEMPLATES_CONFIG + BOTTOM_SV;
    public final static String EMAIL_TEMPLATES_DIRECTORY_SV = HEAD_SV + EMAIL_TEMPLATES_DIRECTORY + BOTTOM_SV;
    public final static String EXPORT_TEMPLATES_SV = HEAD_SV + EXPORT_TEMPLATES + BOTTOM_SV;
    public final static String DATASHIELD_TEMPLATES_SV = HEAD_SV + DATASHIELD_TEMPLATES + BOTTOM_SV;


    // Others
    public final static String TEST_EMAIL = "test@project-manager.com";
    public final static String TEST_BRIDGEHEAD = "bridgehead-test";
    public final static int RANDOM_FILENAME_SIZE = 20;
    public final static int PROJECT_CODE_SIZE = 20;
    public final static int QUERY_CODE_SIZE = 20;
    public final static String NO_BRIDGEHEAD = "NONE";
    public final static String THIS_IS_A_TEST = "This is a test";

}
