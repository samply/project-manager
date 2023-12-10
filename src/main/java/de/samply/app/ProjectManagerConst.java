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

    // Actions
    public final static String SET_DEVELOPER_USER_ACTION = "Set user as developer";
    public final static String SET_PILOT_USER_ACTION = "Set user as pilot";
    public final static String SET_FINAL_USER_ACTION = "Set user as final execution user";
    public final static String DESIGN_PROJECT_ACTION = "Design project";
    public final static String SET_PROJECT_AS_CREATED_ACTION = "Create project";
    public final static String ACCEPT_PROJECT_ACTION = "Accept Project";
    public final static String REJECT_PROJECT_ACTION = "Reject project";
    public final static String ARCHIVE_PROJECT_ACTION = "Archive project";
    public final static String SET_TO_DEVELOP_PROJECT_ACTION = "Set project to develop stage";
    public final static String SET_TO_PILOT_PROJECT_ACTION = "Set project to pilot stage";
    public final static String SET_TO_FINAL_PROJECT_ACTION = "Set project to final stage";
    public final static String FINISH_PROJECT_ACTION = "Finish project";



    // REST Services
    public final static String INFO = "/info";
    public final static String ACTIONS = "/actions";
    public final static String SET_DEVELOPER_USER = "/developer-user";
    public final static String SET_PILOT_USER = "/pilot-user";
    public final static String SET_FINAL_USER = "/final-user";
    public final static String DESIGN_PROJECT = "/draft-project";
    public final static String SET_PROJECT_AS_CREATED = "/create-project";
    public final static String ACCEPT_PROJECT = "/accept-project";
    public final static String REJECT_PROJECT = "/reject-project";
    public final static String ARCHIVE_PROJECT = "/archive-project";
    public final static String SET_TO_DEVELOP_PROJECT = "/develop-project";
    public final static String SET_TO_PILOT_PROJECT = "/pilot-project";
    public final static String SET_TO_FINAL_PROJECT = "/final-project";
    public final static String FINISH_PROJECT = "/finish-project";

    // REST Parameters
    public final static String PROJECT_NAME = "project-name";
    public final static String BRIDGEHEAD = "bridgehead";
    public final static String BRIDGEHEADS = "bridgeheads";
    public final static String SITE = "site";
    public final static String EMAIL = "email";


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

    // Others
    public final static String BRIDGEHEAD_CLAIM = "bridgehead";
    public final static String TEST_EMAIL = "test@project-manager.com";
    public final static String TEST_BRIDGEHEAD = "bridgehead-test";

}
