package de.samply.email;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Data
public class EmailTemplates {

    private Map<EmailTemplateType, EmailMetaData> templates = new HashMap<>();

    @JsonIgnore
    public Optional<TemplateSubject> getTemplateAndSubject(
            @NotNull EmailTemplateType type,
            @NotNull ProjectRole role,
            Optional<String> bridgehead) {

        return Optional
                .ofNullable(templates.get(type))
                .map(meta -> meta.getTemplateAndSubject(role, bridgehead));
    }

    // Whether the email of this type is sent to these recipients at all: only with a template for their role (or
    // DEFAULT), in general or for some bridgehead (TemplateEmailSendingService: no template, no email)
    @JsonIgnore
    public boolean hasTemplate(@NotNull EmailTemplateType type, @NotNull EmailRecipientType recipientType) {
        Set<ProjectRole> roles = recipientType.fetchTemplateRoles();
        return Optional.ofNullable(templates.get(type))
                .map(meta -> Stream.concat(Stream.of(meta), meta.getBridgeheads().values().stream())
                        .map(EmailMetaData::getFiles)
                        .anyMatch(files -> roles.isEmpty() ? !files.isEmpty()
                                : files.containsKey(ProjectRole.DEFAULT) || roles.stream().anyMatch(files::containsKey)))
                .orElse(false);
    }

    public List<String> getAttachmentFiles(
            @NotNull EmailTemplateType type) {

        return Optional
                .ofNullable(templates.get(type))
                .map(EmailMetaData::getAttachmentFiles)
                .orElseGet(List::of);
    }

}
