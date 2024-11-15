package de.samply.frontend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
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
public class ActionExplanation {

    private String module;
    private String site;

    @JsonProperty(value = "project-role")
    private ProjectRole projectRole;
    @JsonProperty(value = "organisation-role")
    private OrganisationRole organisationRole;
    @JsonProperty(value = "project-type")
    private ProjectType projectType;
    @JsonProperty(value = "project-state")
    private ProjectState projectState;
    @JsonProperty(value = "project-bridgehead-state")
    private ProjectBridgeheadState projectBridgeheadState;
    @JsonProperty(value = "query-state")
    private QueryState queryState;

    @JsonProperty(value = "messages", required = true)
    private Map<String, String> languageMessageMap;

}
