package de.samply.form.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.project.state.ProjectState;
import de.samply.utils.LanguageUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Localized, trusted information displayed before or after a form, block group, or field.
 * The configured {@code content} may contain HTML because form configuration is controlled by
 * administrators. Project-state restrictions are resolved by the backend; the frontend renders
 * every non-empty information value it receives.
 *
 * <p>Restricted example:</p>
 * <pre>{@code
 * "pre_info": {
 *   "content": {"en": "Only while preparing the request."},
 *   "project_states": ["DRAFT", "REVIEW"]
 * }
 * }</pre>
 *
 * <p>To display information in every {@link ProjectState}, omit {@code project_states}:</p>
 * <pre>{@code
 * "post_info": {
 *   "content": {"en": "Shown in every project state."}
 * }
 * }</pre>
 *
 * <p>To display information only for certain values of the form, add a {@code condition}: a SpEL
 * expression on the project's form values, with the same syntax as a field's condition. It may
 * refer to the field the information belongs to. The backend evaluates it with the saved values;
 * without project values (e.g. a form not yet filled in) a conditional information is not shown.</p>
 * <pre>{@code
 * "post_info": {
 *   "content": {"en": "No data or material can be shared before the ethics approval."},
 *   "condition": "['ethics']['ethics_approval_status']['value'] == 'pending'"
 * }
 * }</pre>
 *
 * <p>An explicitly empty restriction is invalid and fails application startup:</p>
 * <pre>{@code
 * "pre_info": {
 *   "content": {"en": "Invalid example."},
 *   "project_states": []
 * }
 * }</pre>
 *
 * <p>Restrictions and conditions control only this message. They never hide the associated form, block, or
 * field. Valid values in {@code project_states} are names from {@link ProjectState}.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisplayInfo {

    private Map<String, String> content = new LinkedHashMap<>();

    @JsonProperty("project_states")
    private Set<ProjectState> projectStates;

    /** SpEL condition on the project's form values, as in a field's condition; null = always shown. */
    private String condition;

    public void setContent(Map<String, String> content) {
        this.content = content == null
                ? null
                : content.entrySet().stream().collect(Collectors.toMap(
                        entry -> LanguageUtils.normalize(entry.getKey()),
                        Map.Entry::getValue,
                        (existing, _) -> existing,
                        LinkedHashMap::new));
    }

    public boolean appliesTo(ProjectState projectState) {
        return projectStates == null || projectStates.contains(projectState);
    }

    public boolean isConditional() {
        return condition != null;
    }

}
