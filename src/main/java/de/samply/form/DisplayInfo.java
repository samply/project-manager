package de.samply.form;

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
 * <p>An explicitly empty restriction is invalid and fails application startup:</p>
 * <pre>{@code
 * "pre_info": {
 *   "content": {"en": "Invalid example."},
 *   "project_states": []
 * }
 * }</pre>
 *
 * <p>Restrictions control only this message. They never hide the associated form, block, or
 * field. Valid values in {@code project_states} are names from {@link ProjectState}.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisplayInfo {

    private Map<String, String> content = new LinkedHashMap<>();

    @JsonProperty("project_states")
    private Set<ProjectState> projectStates;

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

}
