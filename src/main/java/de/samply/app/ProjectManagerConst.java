package de.samply.app;

public class ProjectManagerConst {

    public final static String APP_NAME = "Project Manager";

    // Sites
    public final static String PROJECT_DASHBOARD_SITE = "Project Dashboard";
    public final static String PROJECT_VIEW_SITE = "Project View";
    public final static String PROJECT_FORM_SITE = "Project Form";
    public final static String PROJECT_PUBLICATIONS_SITE = "Project Publications";

    //Modules
    public final static String USER_MODULE = "User Module";
    public final static String PROJECT_STATE_MODULE = "Project State Module";
    public final static String PROJECT_DOCUMENTS_MODULE = "Project Documents Module";

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


    // REST Services
    public final static String INFO = "/info";
    public final static String ACTIONS = "/actions";
    public final static String SET_DEVELOPER_USER = "/set-developer-user";
    public final static String SET_PILOT_USER = "/set-pilot-user";
    public final static String SET_FINAL_USER = "/set-final-user";
    public final static String DESIGN_PROJECT = "/design-project";
    public final static String CREATE_PROJECT = "/create-project";
    public final static String ACCEPT_PROJECT = "/accept-project";
    public final static String REJECT_PROJECT = "/reject-project";
    public final static String ARCHIVE_PROJECT = "/archive-project";
    public final static String START_DEVELOP_STAGE = "/start-develop-project";
    public final static String START_PILOT_STAGE = "/start-pilot-project";
    public final static String START_FINAL_STAGE = "/start-final-project";
    public final static String FINISH_PROJECT = "/finish-project";
    public final static String CREATE_PROJECT_QUERY = "/create-project-query";
    public final static String CREATE_PROJECT_CQL_DATA_QUERY = "/create-project-cql-data-query";
    public final static String UPLOAD_PROJECT_DOCUMENT = "/upload-project-document";
    public final static String ADD_PROJECT_DOCUMENT_URL = "/add-project-document-url";
    public final static String DOWNLOAD_PROJECT_DOCUMENT = "/download-project-document";

    // REST Parameters
    public final static String PROJECT_CODE = "project-code";
    public final static String BRIDGEHEAD = "bridgehead";
    public final static String BRIDGEHEADS = "bridgeheads";
    public final static String SITE = "site";
    public final static String EMAIL = "email";
    public final static String QUERY_FORMAT = "query-format";
    public final static String DOCUMENT = "document";
    public final static String DOCUMENT_URL = "document-url";
    public final static String FILENAME = "filename";


    // Application Properties
    public final static String JWT_GROUPS_CLAIM_PROPERTY = "jwt.groups.claim";
    public final static String APP_SECURITY_ENABLED_PROPERTY = "app.security.enabled";

    // Environment Variables
    public final static String PM_ADMIN_GROUPS = "PM_ADMIN_GROUPS";
    public final static String BK_USER_GROUP_PREFIX = "BK_USER_GROUP_PREFIX";
    public final static String BK_USER_GROUP_SUFFIX = "BK_USER_GROUP_SUFFIX";
    public final static String BK_ADMIN_GROUP_PREFIX = "BK_ADMIN_GROUP_PREFIX";
    public final static String BK_ADMIN_GROUP_SUFFIX = "BK_ADMIN_GROUP_SUFFIX";
    public final static String REGISTERED_BRIDGEHEADS = "REGISTERED_BRIDGEHEADS";
    public final static String PROJECT_DOCUMENTS_DIRECTORY = "PROJECT_DOCUMENTS_DIRECTORY";
    public final static String PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT = "PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP";

    // Spring Values (SV)
    public final static String HEAD_SV = "${";
    public final static String BOTTOM_SV = "}";
    public final static String PM_ADMIN_GROUPS_SV = HEAD_SV + PM_ADMIN_GROUPS + BOTTOM_SV;
    public final static String REGISTERED_BRIDGEHEADS_SV = HEAD_SV + REGISTERED_BRIDGEHEADS + BOTTOM_SV;
    public final static String JWT_GROUPS_CLAIM_PROPERTY_SV = HEAD_SV + JWT_GROUPS_CLAIM_PROPERTY + BOTTOM_SV;
    public final static String APP_SECURITY_ENABLED_PROPERTY_SV = HEAD_SV + APP_SECURITY_ENABLED_PROPERTY + ":true" + BOTTOM_SV;
    public final static String BK_USER_GROUP_PREFIX_SV = HEAD_SV + BK_USER_GROUP_PREFIX + BOTTOM_SV;
    public final static String BK_USER_GROUP_SUFFIX_SV = HEAD_SV + BK_USER_GROUP_SUFFIX + BOTTOM_SV;
    public final static String BK_ADMIN_GROUP_PREFIX_SV = HEAD_SV + BK_ADMIN_GROUP_PREFIX + BOTTOM_SV;
    public final static String BK_ADMIN_GROUP_SUFFIX_SV = HEAD_SV + BK_ADMIN_GROUP_SUFFIX + BOTTOM_SV;
    public final static String PROJECT_DOCUMENTS_DIRECTORY_SV = HEAD_SV + PROJECT_DOCUMENTS_DIRECTORY + BOTTOM_SV;
    public final static String PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT_SV = HEAD_SV + PROJECT_DOCUMENTS_DIRECTORY_TIMESTAMP_FORMAT + ":yyyyMMddHHmmss" + BOTTOM_SV;

    // Others
    public final static String TEST_EMAIL = "test@project-manager.com";
    public final static String TEST_BRIDGEHEAD = "bridgehead-test";
    public final static int RANDOM_FILENAME_SIZE = 20;

}
