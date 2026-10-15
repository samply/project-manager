package de.samply.frontend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.project.state.UserProjectState;
import de.samply.query.QueryState;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.ProjectRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionMessage {

    private String module;
    private String site;

    @JsonProperty("project-role")
    private ProjectRole projectRole;
    @JsonProperty("organisation-role")
    private OrganisationRole organisationRole;
    @JsonProperty("project-type")
    private ProjectType projectType;
    @JsonProperty("project-state")
    private ProjectState projectState;
    @JsonProperty("project-bridgehead-state")
    private ProjectBridgeheadState projectBridgeheadState;
    @JsonProperty("query-state")
    private QueryState queryState;
    @JsonProperty("user-project-state")
    private UserProjectState userProjectState;
    private Integer priority;

    @JsonProperty("explanation")
    @JsonDeserialize(using = LanguageMessageMapDeserializer.class)
    private Map<String, String> explanationMessages;

    @JsonProperty("success-message")
    @JsonDeserialize(using = LanguageMessageMapDeserializer.class)
    private Map<String, String> successMessages;

    @JsonProperty("error-message")
    @JsonDeserialize(using = LanguageMessageMapDeserializer.class)
    private Map<String, String> errorMessages;
}
