package de.samply.app;

public class ProjectManagerConst {

    public final static String APP_NAME = "Project Manager";

    // REST Services
    public final static String INFO = "/info";
    public final static String ACTIONS = "/actions";


    // Application Properties
    public final static String PM_ADMIN_GROUP_PROPERTY = "project-manager-admin";
    public final static String JWT_GROUPS_CLAIM_PROPERTY = "jwt.groups.claim";

    // Environment Variables
    public final static String BK_USER_GROUP_PREFIX = "BK_USER_GROUP_PREFIX";
    public final static String BK_USER_GROUP_SUFFIX = "BK_USER_GROUP_SUFFIX";
    public final static String BK_ADMIN_GROUP_PREFIX = "BK_ADMIN_GROUP_PREFIX";
    public final static String BK_ADMIN_GROUP_SUFFIX = "BK_ADMIN_GROUP_SUFFIX";

    // Spring Values (SV)
    public final static String HEAD_SV = "${";
    public final static String BOTTOM_SV = "}";
    public final static String JWT_GROUPS_CLAIM_PROPERTY_SV = HEAD_SV + JWT_GROUPS_CLAIM_PROPERTY + BOTTOM_SV;
    public final static String BK_USER_GROUP_PREFIX_SV = HEAD_SV + BK_USER_GROUP_PREFIX + BOTTOM_SV;
    public final static String BK_USER_GROUP_SUFFIX_SV = HEAD_SV + BK_USER_GROUP_SUFFIX + BOTTOM_SV;
    public final static String BK_ADMIN_GROUP_PREFIX_SV = HEAD_SV + BK_ADMIN_GROUP_PREFIX + BOTTOM_SV;
    public final static String BK_ADMIN_GROUP_SUFFIX_SV = HEAD_SV + BK_ADMIN_GROUP_SUFFIX + BOTTOM_SV;

    // Others

}
