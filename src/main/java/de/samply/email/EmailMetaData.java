package de.samply.email;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.samply.user.roles.ProjectRole;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
public class EmailMetaData {
    private String subject;
    @JsonProperty("attachment_files")
    private List<String> attachmentFiles = new ArrayList<>();
    private Map<ProjectRole, String> files = new HashMap<>();
    // Templates of one bridgehead (key: bridgehead id), e.g. for a site's own local approval process: subject and files
    // as above; without a template for the role there, the general one is used
    private Map<String, EmailMetaData> bridgeheads = new HashMap<>();

    @JsonIgnore
    public TemplateSubject getTemplateAndSubject(ProjectRole role) {
        return getTemplateAndSubject(role, Optional.empty());
    }

    // Order: bridgehead + role, bridgehead + DEFAULT, role, DEFAULT
    @JsonIgnore
    public TemplateSubject getTemplateAndSubject(ProjectRole role, Optional<String> bridgehead) {
        return bridgehead
                .map(bridgeheads::get)
                .flatMap(bridgeheadMetaData -> bridgeheadMetaData.fetchTemplate(role)
                        .map(template -> new TemplateSubject(template,
                                bridgeheadMetaData.subject != null ? bridgeheadMetaData.subject : subject)))
                .or(() -> fetchTemplate(role).map(template -> new TemplateSubject(template, subject)))
                .orElse(null);
    }

    private Optional<String> fetchTemplate(ProjectRole role) {
        return Optional.ofNullable(files.get(role)).or(() -> Optional.ofNullable(files.get(ProjectRole.DEFAULT)));
    }

}
