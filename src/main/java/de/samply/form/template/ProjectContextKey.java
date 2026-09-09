package de.samply.form.template;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum ProjectContextKey {

    PROJECT_CODE("project-code"),
    CREATOR_BRIDGEHEADS("creator-bridgeheads"),
    CREATOR_AFFILIATIONS("creator-affiliations"),
    CREATOR_NAME("creator-name"),
    // A creator without any affiliation must not show a dangling/empty
    // "(...)" - since ProjectContext's placeholder substitution has no
    // conditional logic, that formatting decision is made once here instead
    // of composed from CREATOR_NAME + CREATOR_AFFILIATIONS in configuration
    // (2026-09-09 feedback point 1).
    CREATOR_NAME_WITH_AFFILIATIONS("creator-name-with-affiliations"),
    CREATOR_EMAIL("creator-email"),
    PROJECT_TITLE("project-title"),
    PROJECT_DESCRIPTION("project-description"),
    PROJECT_CREATION_DATE("project-creation-date"),
    ETHICAL_APPROVAL("ethical-approval"),
    // F10/Phase 7 (plan-pdf-form-field-parity-2026-09-07.md): native/FIXED
    // fields the frontend shows outside the dynamic form-field system,
    // resolved here the same way PROJECT_TITLE/PROJECT_DESCRIPTION already
    // are, so a form template's project_fields can reference them.
    ETHICS_VOTE_FOR_ALL_SITES_FILENAME("ethics-vote-for-all-sites-filename"),
    DESCRIPTION_UPLOAD_FILENAME("description-upload-filename"),
    ENVIRONMENT_VARIABLES("environment-variables"),
    QUERY_FORMAT("query-format"),
    ADDITIONAL_FILTER_CRITERIA("additional-filter-criteria"),
    SELECTED_COHORT("selected-cohort"),
    // Phase 8: structured native fields - joined into one string per key
    // (comma- or newline-separated), since project_fields configuration only
    // substitutes one value per key, not a dynamic number of rows.
    QUERIED_SITES("queried-sites"),
    ETHICS_VOTES_PER_SITE("ethics-votes-per-site"),
    PROJECT_TYPES("project-types"),
    OUTPUT_FORMATS("output-formats"),
    TEMPLATE_IDS("template-ids"),
    PROJECT_CONFIGURATION("project-configuration");

    private final String text;

    private static final Map<String, ProjectContextKey> BY_TEXT =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            ProjectContextKey::getText,
                            Function.identity()
                    ));

    ProjectContextKey(String text) {
        this.text = text;
    }

    public static ProjectContextKey fromText(String text) {
        return BY_TEXT.get(text);
    }

}
