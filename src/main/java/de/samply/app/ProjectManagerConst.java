package de.samply.app;

public class ProjectManagerConst {

    public final static String APP_NAME = "Project Manager";

    // Sites
    public final static String PROJECT_DASHBOARD_SITE = "project-dashboard";
    public final static String PROJECT_VIEW_SITE = "project-view";

    //Modules
    public final static String USER_MODULE = "User Module";
    public final static String PROJECT_STATE_MODULE = "Project State Module";
    public final static String PROJECT_DOCUMENTS_MODULE = "Project Documents Module";
    public final static String EXPORT_MODULE = "Export Module";
    public final static String TOKEN_MANAGER_MODULE = "Token Manager Module";

    // Actions
    public final static String SET_DEVELOPER_USER_ACTION = "Set user as developer";
    public final static String SET_PILOT_USER_ACTION = "Set user as pilot";
    public final static String SET_FINAL_USER_ACTION = "Set user as final execution user";
    public final static String DESIGN_PROJECT_ACTION = "Design project";
    public final static String CREATE_PROJECT_ACTION = "Create project";
    public final static String ACCEPT_PROJECT_ACTION = "Accept project";
    public final static String REJECT_PROJECT_ACTION = "Reject project";
    public final static String ARCHIVE_PROJECT_ACTION = "Archive project";
    public final static String START_DEVELOP_STAGE_ACTION = "Start develop stage";
    public final static String START_PILOT_STAGE_ACTION = "Start pilot stage";
    public final static String START_FINAL_STAGE_ACTION = "Start final stage";
    public final static String FINISH_PROJECT_ACTION = "Finish project";
    public final static String UPLOAD_PROJECT_DOCUMENT_ACTION = "Upload project document";
    public final static String ADD_PROJECT_DOCUMENT_URL_ACTION = "Add project document URL";
    public final static String DOWNLOAD_PROJECT_DOCUMENT_ACTION = "Download project document";
    public final static String SAVE_QUERY_IN_BRIDGEHEAD_ACTION = "Save query in bridgehead";
    public final static String SAVE_AND_EXECUTE_QUERY_IN_BRIDGEHEAD_ACTION = "Save and execute query in bridgehead";
    public final static String FETCH_AUTHENTICATION_SCRIPT_ACTION = "Fetch authentication script";
    public final static String CREATE_QUERY_AND_DESIGN_PROJECT_ACTION = "Create Query and Design Project";

    // REST Services
    public final static String INFO = "/info";
    public final static String ACTIONS = "/actions";
    public final static String SET_DEVELOPER_USER = "/set-developer-user";
    public final static String SET_PILOT_USER = "/set-pilot-user";
    public final static String SET_FINAL_USER = "/set-final-user";
    public final static String CREATE_QUERY_AND_DESIGN_PROJECT = "/create-query-and-design-project";
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


    // Application Properties
    public final static String JWT_GROUPS_CLAIM_PROPERTY = "jwt.groups.claim";
    public final static String SECURITY_ENABLED = "SECURITY_ENABLED";
    public final static String IS_TEST_ENVIRONMENT = "IS_TEST_ENVIRONMENT";

    // Exporter Variables
    public final static String EXPORTER_REQUEST = "/request";
    public final static String EXPORTER_CREATE_QUERY = "/create-query";
    public final static String EXPORTER_LOGS = "/logs";
    public static final String EXPORTER_LOGS_SIZE = "logs-size";
    public static final String EXPORTER_LOGS_LAST_LINE = "logs-last-line";
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
    public final static String IS_INTERNAL_REQUEST = "internal-request";


    // Environment Variables
    public final static String PM_ADMIN_GROUPS = "PM_ADMIN_GROUPS";
    public final static String BK_USER_GROUP_PREFIX = "BK_USER_GROUP_PREFIX";
    public final static String BK_USER_GROUP_SUFFIX = "BK_USER_GROUP_SUFFIX";
    public final static String BK_ADMIN_GROUP_PREFIX = "BK_ADMIN_GROUP_PREFIX";
    public final static String BK_ADMIN_GROUP_SUFFIX = "BK_ADMIN_GROUP_SUFFIX";
    public final static String REGISTERED_BRIDGEHEADS = "bridgeheads";
    public final static String FRONTEND_CONFIG = "frontend";
    public final static String PROJECT_DOCUMENTS_DIRECTORY = "PROJECT_DOCUMENTS_DIRECTORY";
    public final static String PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT = "PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP";
    public final static String WEBCLIENT_BUFFER_SIZE_IN_BYTES = "WEBCLIENT_BUFFER_SIZE_IN_BYTES";
    public final static String WEBCLIENT_REQUEST_TIMEOUT_IN_SECONDS = "WEBCLIENT_REQUEST_TIMEOUT_IN_SECONDS";
    public final static String WEBCLIENT_CONNECTION_TIMEOUT_IN_SECONDS = "WEBCLIENT_CONNECTION_TIMEOUT_IN_SECONDS";
    public final static String WEBCLIENT_TCP_KEEP_IDLE_IN_SECONDS = "WEBCLIENT_TCP_KEEP_IDLE_IN_SECONDS";
    public final static String WEBCLIENT_TCP_KEEP_INTERVAL_IN_SECONDS = "WEBCLIENT_TCP_KEEP_INTERVAL_IN_SECONDS";
    public final static String WEBCLIENT_TCP_KEEP_CONNECTION_NUMBER_OF_TRIES = "WEBCLIENT_TCP_KEEP_CONNECTION_NUMBER_OF_TRIES";
    public final static String WEBCLIENT_MAX_NUMBER_OF_RETRIES = "WEBCLIENT_MAX_NUMBER_OF_RETRIES";
    public final static String WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE = "WEBCLIENT_TIME_IN_SECONDS_AFTER_RETRY_WITH_FAILURE";
    public final static String EXPORTER_URL = "EXPORTER_URL";
    public final static String EXPORTER_API_KEY = "EXPORTER_API_KEY";
    public final static String PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS = "PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS";


    // Spring Values (SV)
    public final static String HEAD_SV = "${";
    public final static String BOTTOM_SV = "}";
    public final static String PM_ADMIN_GROUPS_SV = HEAD_SV + PM_ADMIN_GROUPS + BOTTOM_SV;
    //public final static String REGISTERED_BRIDGEHEADS_SV = HEAD_SV + REGISTERED_BRIDGEHEADS + BOTTOM_SV;
    public final static String JWT_GROUPS_CLAIM_PROPERTY_SV = HEAD_SV + JWT_GROUPS_CLAIM_PROPERTY + BOTTOM_SV;
    public final static String BK_USER_GROUP_PREFIX_SV = HEAD_SV + BK_USER_GROUP_PREFIX + BOTTOM_SV;
    public final static String BK_USER_GROUP_SUFFIX_SV = HEAD_SV + BK_USER_GROUP_SUFFIX + BOTTOM_SV;
    public final static String BK_ADMIN_GROUP_PREFIX_SV = HEAD_SV + BK_ADMIN_GROUP_PREFIX + BOTTOM_SV;
    public final static String BK_ADMIN_GROUP_SUFFIX_SV = HEAD_SV + BK_ADMIN_GROUP_SUFFIX + BOTTOM_SV;
    public final static String PROJECT_DOCUMENTS_DIRECTORY_SV = HEAD_SV + PROJECT_DOCUMENTS_DIRECTORY + BOTTOM_SV;
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
    public final static String EXPORTER_URL_SV = HEAD_SV + EXPORTER_URL + BOTTOM_SV;
    public final static String EXPORTER_API_KEY_SV = HEAD_SV + EXPORTER_API_KEY + BOTTOM_SV;
    public final static String PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS_SV =
            HEAD_SV + PROJECT_DEFAULT_EXPIRATION_TIME_IN_DAYS + ":90" + BOTTOM_SV;


    // Others
    public final static String TEST_EMAIL = "test@project-manager.com";
    public final static String TEST_BRIDGEHEAD = "bridgehead-test";
    public final static int RANDOM_FILENAME_SIZE = 20;
    public final static int PROJECT_CODE_SIZE = 20;
    public final static int QUERY_CODE_SIZE = 20;
    public final static String NO_BRIDGEHEAD = "NONE";

}
