package de.samply.email;

import de.samply.annotations.ConditionalOnModuleDisabled;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.modules.OptionalModule;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Used while the module EMAILS is disabled: logs the email that would have been sent, as before with
 * ENABLE_EMAILS=false.
 */
@Slf4j
@Service
@ConditionalOnModuleDisabled(OptionalModule.EMAILS)
public class DisabledEmailSendingService implements EmailSendingService {

    @Override
    public void sendEmail(@NotNull String emailTo, Optional<Project> project, Optional<ProjectBridgehead> bridgehead,
                          @NotNull ProjectRole role, @NotNull EmailTemplateType type) {
        logNotSent(emailTo, project, bridgehead, role, type);
    }

    @Override
    public void sendEmail(@NotNull String emailTo, Optional<Project> project, Optional<ProjectBridgehead> bridgehead,
                          @NotNull ProjectRole role, @NotNull EmailTemplateType type, EmailKeyValues keyValues) {
        logNotSent(emailTo, project, bridgehead, role, type);
    }

    private void logNotSent(String emailTo, Optional<Project> project, Optional<ProjectBridgehead> bridgehead,
                            ProjectRole role, EmailTemplateType type) {
        log.info("Emails are disabled (ENABLE_EMAILS=false): email to {} with role {} of type {} for project {} and bridgehead {} not sent",
                emailTo, role, type, project.map(Project::getCode).orElse("NONE"),
                bridgehead.map(ProjectBridgehead::getBridgehead).orElse("NONE"));
    }

}
