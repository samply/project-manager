package de.samply.token;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadDataShield;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.ProjectBridgeheadDataShieldRepository;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.email.EmailService;
import de.samply.email.EmailServiceException;
import de.samply.email.EmailTemplateType;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
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
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository;
    private final EmailService emailService;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final boolean isTokenManagerActive;

    public DataShieldTokenManagerJob(DataShieldTokenManagerService tokenManagerService,
                                     ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                                     ProjectBridgeheadRepository projectBridgeheadRepository,
                                     ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository,
                                     EmailService emailService, BridgeheadConfiguration bridgeheadConfiguration,
                                     @Value(ProjectManagerConst.ENABLE_TOKEN_MANAGER_SV) boolean isTokenManagerActive
    ) {
        this.tokenManagerService = tokenManagerService;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectBridgeheadDataShieldRepository = projectBridgeheadDataShieldRepository;
        this.emailService = emailService;
        this.bridgeheadConfiguration = bridgeheadConfiguration;
        this.isTokenManagerActive = isTokenManagerActive;
    }

    @Scheduled(cron = ProjectManagerConst.MANAGE_TOKENS_CRON_EXPRESSION_SV)
    public void manageTokens() {
        if (isTokenManagerActive) {
            manageActiveUsers();
            manageInactiveUsers();
            manageInactiveProjects();
        }
    }

    private void manageActiveUsers() {
        // Get active users of active DataSHIELD projects
        List<ProjectBridgeheadUser> activeUsers = this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndProjectRole(ProjectType.DATASHIELD, ProjectState.DEVELOP, ProjectBridgeheadState.ACCEPTED, ProjectRole.DEVELOPER);
        activeUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndProjectRole(ProjectType.DATASHIELD, ProjectState.PILOT, ProjectBridgeheadState.ACCEPTED, ProjectRole.PILOT));
        activeUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndProjectRole(ProjectType.DATASHIELD, ProjectState.FINAL, ProjectBridgeheadState.ACCEPTED, ProjectRole.FINAL));
        Set<ProjectEmail> usersToSendAnEmail = new HashSet<>();
        activeUsers.forEach(user -> tokenManagerService.fetchProjectBridgeheads(user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead(), user.getEmail()).stream().filter(this::isBridgeheadConfiguredForTokenManager).forEach(bridgehead -> {
            // Check user status
            DataShieldTokenManagerTokenStatus dataShieldTokenManagerTokenStatus = tokenManagerService.fetchTokenStatus(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail());
            if (dataShieldTokenManagerTokenStatus.projectStatus() == DataShieldProjectStatus.WITH_DATA) {
                if (dataShieldTokenManagerTokenStatus.tokenStatus() == DataShieldTokenStatus.NOT_FOUND) { // If user token not found: Create token
                    tokenManagerService.generateTokensInOpal(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail());
                    usersToSendAnEmail.add(new ProjectEmail(user.getEmail(), user.getProjectBridgehead().getProject().getCode(), user.getProjectRole()));
                } else if (dataShieldTokenManagerTokenStatus.tokenStatus() == DataShieldTokenStatus.EXPIRED) { // If user token expired: Refresh Token
                    tokenManagerService.refreshToken(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail());
                    usersToSendAnEmail.add(new ProjectEmail(user.getEmail(), user.getProjectBridgehead().getProject().getCode(), user.getProjectRole()));
                }
            }
        }));
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
        List<ProjectBridgeheadUser> inactiveUsers = this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectBridgeheadState(ProjectType.DATASHIELD, ProjectState.DEVELOP, ProjectBridgeheadState.ACCEPTED);
        inactiveUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectBridgeheadState(ProjectType.DATASHIELD, ProjectState.PILOT, ProjectBridgeheadState.ACCEPTED));
        inactiveUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectBridgeheadState(ProjectType.DATASHIELD, ProjectState.FINAL, ProjectBridgeheadState.ACCEPTED));
        inactiveUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.DEVELOP, ProjectRole.DEVELOPER));
        inactiveUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.PILOT, ProjectRole.PILOT));
        inactiveUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.FINAL, ProjectRole.FINAL));
        inactiveUsers.stream().filter(user -> user.getProjectRole() != ProjectRole.CREATOR).forEach(user -> {
            this.tokenManagerService.fetchProjectBridgeheads(user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead(), user.getEmail()).stream().filter(this::isBridgeheadConfiguredForTokenManager).forEach(bridgehead -> {
                // Check user status
                DataShieldTokenManagerTokenStatus dataShieldTokenManagerTokenStatus = tokenManagerService.fetchTokenStatus(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail());
                // If user token created or expired: Remove token
                if (dataShieldTokenManagerTokenStatus.tokenStatus() != DataShieldTokenStatus.NOT_FOUND) {
                    tokenManagerService.removeTokens(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail());
                }
            });
        });
    }

    private void manageInactiveProjects() {
        // Get users of DataSHIELD inactive states projects
        List<ProjectBridgehead> inactiveProjects = this.projectBridgeheadRepository.getByProjectTypeAndNotProjectState(ProjectType.DATASHIELD, Set.of(ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL));
        Set<String> removedProjects = new HashSet<>();
        inactiveProjects.stream().filter(this::isBridgeheadConfiguredForTokenManager).forEach(projectBridgehead -> {
            Optional<ProjectBridgeheadDataShield> projectBridgeheadInDataShield = projectBridgeheadDataShieldRepository.findByProjectBridgehead(projectBridgehead);
            if (projectBridgeheadInDataShield.isEmpty() || !projectBridgeheadInDataShield.get().isRemoved()) {
                // Check user status
                DataShieldTokenManagerProjectStatus dataShieldTokenManagerProjectStatus = tokenManagerService.fetchProjectStatus(projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead());
                // if project not found: Remove Token and project
                if (dataShieldTokenManagerProjectStatus.projectStatus() != DataShieldProjectStatus.NOT_FOUND) {
                    tokenManagerService.removeProjectAndTokens(projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead());
                }
            }
            setAsRemoved(projectBridgeheadInDataShield, projectBridgehead);
        });
    }

    private boolean isBridgeheadConfiguredForTokenManager(ProjectBridgehead projectBridgehead) {
        return isBridgeheadConfiguredForTokenManager(projectBridgehead.getBridgehead());
    }

    private boolean isBridgeheadConfiguredForTokenManager(String bridgehead) {
        return this.bridgeheadConfiguration.getTokenManagerId(bridgehead).isPresent();
    }

    private void setAsRemoved(Optional<ProjectBridgeheadDataShield> projectBridgeheadInDataShield, ProjectBridgehead projectBridgehead) {
        ProjectBridgeheadDataShield result = null;
        if (projectBridgeheadInDataShield.isPresent()) {
            if (!projectBridgeheadInDataShield.get().isRemoved()) {
                result = projectBridgeheadInDataShield.get();
                result.setRemoved(true);
            }
        } else {
            result = new ProjectBridgeheadDataShield();
            result.setProjectBridgehead(projectBridgehead);
        }
        if (result != null) {
            projectBridgeheadDataShieldRepository.save(result);
        }
    }

}
