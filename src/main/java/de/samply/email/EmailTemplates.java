package de.samply.email;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class EmailTemplates {


    private Map<EmailTemplateType, EmailMetaData> templates = new HashMap<>();

    @JsonIgnore
    public Optional<TemplateSubject> getTemplateAndSubject(@NotNull EmailTemplateType type, @NotNull ProjectRole role) {
        EmailMetaData emailMetaData = templates.get(type);
        if (emailMetaData != null) {
            return Optional.ofNullable(emailMetaData.getTemplateAndSubject(role));
        }
        return Optional.empty();
    }

}
