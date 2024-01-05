package de.samply.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class TokenParams {

    @JsonProperty("email")
    private String email;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("bridgehead_ids")
    private List<String> bridgeheadIds;

    public TokenParams() {
    }

    public TokenParams(String email, String projectId, List<String> bridgeheadIds) {
        this.email = email;
        this.projectId = projectId;
        this.bridgeheadIds = bridgeheadIds;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public List<String> getBridgeheadIds() {
        return bridgeheadIds;
    }

    public void setBridgeheadIds(List<String> bridgeheadIds) {
        this.bridgeheadIds = bridgeheadIds;
    }

    @Override
    public String toString() {
        return "TokenParams{" +
                "email='" + email + '\'' +
                ", project_id='" + projectId + '\'' +
                ", bridgehead_ids=" + bridgeheadIds +
                '}';
    }
}
