package de.samply.token;

import de.samply.app.ProjectManagerConst;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.email.EmailService;
import de.samply.email.EmailServiceException;
import de.samply.email.EmailTemplateType;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectState;
import de.samply.token.dto.DataShieldProjectStatus;
import de.samply.token.dto.DataShieldTokenManagerProjectStatus;
import de.samply.token.dto.DataShieldTokenManagerTokenStatus;
import de.samply.token.dto.DataShieldTokenStatus;
import de.samply.user.roles.ProjectRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class DataShieldTokenManagerJob {

    private final DataShieldTokenManagerService tokenManagerService;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final EmailService emailService;
    private final boolean isTokenManagerActive;

    public DataShieldTokenManagerJob(DataShieldTokenManagerService tokenManagerService,
                                     ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                                     EmailService emailService,
                                     @Value(ProjectManagerConst.ENABLE_TOKEN_MANAGER_SV) boolean isTokenManagerActive
    ) {
        this.tokenManagerService = tokenManagerService;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.emailService = emailService;
        this.isTokenManagerActive = isTokenManagerActive;
    }

    @Scheduled(cron = ProjectManagerConst.MANAGE_TOKENS_CRON_EXPRESSION_SV)
    public void manageTokens() {
        if (isTokenManagerActive){
            manageActiveUsers();
            manageInactiveUsers();
            manageInactiveProjects();
        }
    }

    private void manageActiveUsers() {
        // Get active users of active DataSHIELD projects
        List<ProjectBridgeheadUser> activeUsers = this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndProjectRole(ProjectType.DATASHIELD, ProjectState.DEVELOP, ProjectRole.DEVELOPER);
        activeUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndProjectRole(ProjectType.DATASHIELD, ProjectState.PILOT, ProjectRole.PILOT));
        activeUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndProjectRole(ProjectType.DATASHIELD, ProjectState.FINAL, ProjectRole.FINAL));
        Set<ProjectEmail> usersToSendAnEmail = new HashSet<>();
        activeUsers.forEach(user -> {
            // Check user status
            DataShieldTokenManagerTokenStatus dataShieldTokenManagerTokenStatus = tokenManagerService.fetchTokenStatus(user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead(), user.getEmail());
            if (dataShieldTokenManagerTokenStatus.projectStatus() == DataShieldProjectStatus.WITH_DATA) {
                if (dataShieldTokenManagerTokenStatus.tokenStatus() == DataShieldTokenStatus.NOT_FOUND) { // If user token not found: Create token
                    tokenManagerService.generateTokensInOpal(user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead(), user.getEmail());
                    usersToSendAnEmail.add(new ProjectEmail(user.getEmail(), user.getProjectBridgehead().getProject().getCode(), user.getProjectRole()));
                } else if (dataShieldTokenManagerTokenStatus.tokenStatus() == DataShieldTokenStatus.EXPIRED) { // If user token expired: Refresh Token
                    tokenManagerService.refreshToken(user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead(), user.getEmail());
                    usersToSendAnEmail.add(new ProjectEmail(user.getEmail(), user.getProjectBridgehead().getProject().getCode(), user.getProjectRole()));
                }
            }
        });
        usersToSendAnEmail.forEach(userProject -> sendEmail(userProject.getEmail(), EmailTemplateType.NEW_TOKEN_FOR_AUTHENTICATION_SCRIPT, userProject.getProjectRole()));
    }

    private void sendEmail(String email, EmailTemplateType type, ProjectRole projectRole) {
        try {
            emailService.sendEmail(email, Optional.empty(), projectRole, type); //TODO: Add project code
        } catch (EmailServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private void manageInactiveUsers() {
        // Get inactive users of active DataSHIELD projects
        List<ProjectBridgeheadUser> inactiveUsers = this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.DEVELOP, ProjectRole.DEVELOPER);
        inactiveUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.PILOT, ProjectRole.PILOT));
        inactiveUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.FINAL, ProjectRole.FINAL));
        inactiveUsers.forEach(user -> {
            // Check user status
            DataShieldTokenManagerTokenStatus dataShieldTokenManagerTokenStatus = tokenManagerService.fetchTokenStatus(user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead(), user.getEmail());
            // If user token created or expired: Remove token
            if (dataShieldTokenManagerTokenStatus.tokenStatus() != DataShieldTokenStatus.NOT_FOUND) {
                tokenManagerService.removeTokens(user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead(), user.getEmail());
            }
        });
    }

    private void manageInactiveProjects() {
        // Get users of DataSHIELD inactive states projects
        List<ProjectBridgeheadUser> inactiveProjectsUsers = this.projectBridgeheadUserRepository.getByProjectTypeAndNotProjectState(ProjectType.DATASHIELD, Set.of(ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL));
        Set<String> removedProjects = new HashSet<>();
        inactiveProjectsUsers.forEach(user -> {
            if (!removedProjects.contains(user.getProjectBridgehead().getProject().getCode())) {
                removedProjects.add(user.getProjectBridgehead().getProject().getCode());
                // Check user status
                DataShieldTokenManagerProjectStatus dataShieldTokenManagerProjectStatus = tokenManagerService.fetchProjectStatus(user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead());
                // if project not found: Remove Token and project
                if (dataShieldTokenManagerProjectStatus.projectStatus() != DataShieldProjectStatus.NOT_FOUND) {
                    tokenManagerService.removeProjectAndTokens(user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead());
                }
            }
        });
    }


}
