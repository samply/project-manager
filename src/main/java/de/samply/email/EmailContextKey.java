package de.samply.email;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum EmailContextKey {

    BRIDGEHEAD("bridgehead"),
    PROJECT_CODE("project-code"),
    PROJECT_BRIDGEHEADS("project-bridgeheads"),
    PROJECT_BRIDGEHEAD_USER_EMAIL("project-bridgehead-user-email"),
    PROJECT_BRIDGEHEAD_USER_FIRST_NAME("project-bridgehead-user-first-name"),
    PROJECT_BRIDGEHEAD_USER_LAST_NAME("project-bridgehead-user-last-name"),
    PROJECT_BRIDGEHEAD_USER_NAME("project-bridgehead-user-name"),
    PROJECT_BRIDGEHEAD_RESULTS_URL("project-bridgehead-results-url"),
    PROJECT_ROLE("project-role"),
    PROJECT_TYPE("project-type"),
    PROJECT_CREATOR_EMAIL("project-creator-email"),
    PROJECT_CREATOR_FIRST_NAME("project-creator-first-name"),
    PROJECT_CREATOR_LAST_NAME("project-creator-last-name"),
    PROJECT_CREATOR_NAME("project-creator-name"),
    PROJECT_RESULTS_URL("project-results-url"),
    QUERY("query"),
    QUERY_LABEL("query-label"),
    QUERY_DESCRIPTION("query-description"),
    PROJECT_VIEW_URL("project-view-url"),
    MESSAGE("message"),
    EMAIL_TO("email-to"),
    EMAIL_TO_FIRST_NAME("email-to-first-name"),
    EMAIL_TO_LAST_NAME("email-to-last-name"),
    EMAIL_TO_NAME("email-to-name"),
    LAST_DOCUMENT_LABEL("last-document-label"),
    LAST_DOCUMENT_FILENAME("last-document-filename"),
    LAST_DOCUMENT_URL("last-document-url"),
    LAST_DOCUMENT_SENDER_NAME("last-document-url-sender-name"),
    LAST_DOCUMENT_SENDER_FIRST_NAME("last-document-url-sender-first-name"),
    LAST_DOCUMENT_SENDER_LAST_NAME("last-document-url-sender-last-name"),
    LAST_DOCUMENT_SENDER_EMAIL("last-document-url-sender-email"),
    BRIDGEHEAD_ADMIN_FIRST_NAME("bridgehead-admin-first-name"),
    BRIDGEHEAD_ADMIN_LAST_NAME("bridgehead-admin-last-name"),
    BRIDGEHEAD_ADMIN_NAME("bridgehead-admin-name"),
    BRIDGEHEAD_ADMIN_EMAIL("bridgehead-admin-email"),
    RESEARCH_ENVIRONMENT_URL("research-environment-url");

    private String value;
    private static Set<String> allValues = Stream.of(values()).map(EmailContextKey::getValue).collect(Collectors.toSet());

    EmailContextKey(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Set<String> getAllValues() {
        return allValues;
    }

}
