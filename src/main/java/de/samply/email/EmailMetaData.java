package de.samply.email;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.user.roles.ProjectRole;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class EmailMetaData {
    private String subject;
    @JsonProperty("attachment_files")
    private List<String> attachmentFiles = new ArrayList<>();
    private Map<ProjectRole, String> files = new HashMap<>();

    @JsonIgnore
    public TemplateSubject getTemplateAndSubject(ProjectRole role) {
        String template = files.get(role);
        if (template == null) {
            template = files.get(ProjectRole.DEFAULT);
        }
        return (template != null) ? new TemplateSubject(template, subject) : null;
    }

}
