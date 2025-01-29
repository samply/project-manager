package de.samply.token;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.coder.CoderService;
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
import de.samply.rstudio.group.RstudioGroupService;
import de.samply.token.dto.DataShieldProjectStatus;
import de.samply.token.dto.DataShieldTokenManagerProjectStatus;
import de.samply.token.dto.DataShieldTokenManagerTokenStatus;
import de.samply.token.dto.DataShieldTokenStatus;
import de.samply.user.roles.ProjectRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class DataShieldTokenManagerJob {

    private final RstudioGroupService rstudioGroupService;
    private final CoderService coderService;
    private final DataShieldTokenManagerService tokenManagerService;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository;
    private final EmailService emailService;
    private final BridgeheadConfiguration bridgeheadConfiguration;
    private final boolean isTokenManagerActive;

    public DataShieldTokenManagerJob(RstudioGroupService rstudioGroupService,
                                     CoderService coderService,
                                     DataShieldTokenManagerService tokenManagerService,
                                     ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                                     ProjectBridgeheadRepository projectBridgeheadRepository,
                                     ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository,
                                     EmailService emailService, BridgeheadConfiguration bridgeheadConfiguration,
                                     @Value(ProjectManagerConst.ENABLE_TOKEN_MANAGER_SV) boolean isTokenManagerActive
    ) {
        this.rstudioGroupService = rstudioGroupService;
        this.coderService = coderService;
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
        List<Mono<Object>> tokenGenerations = new ArrayList<>();
        Set<ProjectEmail> usersToSendAnEmail = new HashSet<>();
        // Get active users of active DataSHIELD projects
        fetchActiveUsersOfDataShieldProjectsInDevelopPilotAndFinalState().forEach(user ->
                tokenManagerService.fetchProjectBridgeheads(
                        user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead(), user.getEmail(),
                        projectBridgehead -> projectBridgehead.getState() == ProjectBridgeheadState.ACCEPTED
                ).stream().filter(this::isBridgeheadConfiguredForTokenManager).forEach(bridgehead -> {
                    // Check user status
                    DataShieldTokenManagerTokenStatus dataShieldTokenManagerTokenStatus = tokenManagerService.fetchTokenStatus(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail());
                    if (dataShieldTokenManagerTokenStatus.projectStatus() == DataShieldProjectStatus.WITH_DATA) {
                        Runnable ifSuccessRunnable = () -> sendNewTokenEmailAndCreateWorkspaceIfNotExists(user.getEmail(), user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getProjectRole(), usersToSendAnEmail);
                        if (dataShieldTokenManagerTokenStatus.tokenStatus() == DataShieldTokenStatus.NOT_FOUND) { // If user token not found: Create token
                            tokenGenerations.add(tokenManagerService.generateTokensInOpal(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail(), ifSuccessRunnable));
                        } else if (dataShieldTokenManagerTokenStatus.tokenStatus() == DataShieldTokenStatus.EXPIRED) { // If user token expired: Refresh Token
                            tokenGenerations.add(tokenManagerService.refreshToken(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail(), ifSuccessRunnable));
                        }
                    }
                }));
        Mono.when(tokenGenerations).block(); // Assure that the whole processes are executed.
    }

    private void sendNewTokenEmailAndCreateWorkspaceIfNotExists(String email, String projectCode, String bridgehead, ProjectRole projectRole, Set<ProjectEmail> usersToSendAnEmail) {
        ProjectEmail projectEmail = new ProjectEmail(projectCode, bridgehead);
        if (!usersToSendAnEmail.contains(projectEmail)) {
            usersToSendAnEmail.add(projectEmail);
            sendEmail(email, projectCode, bridgehead, EmailTemplateType.NEW_TOKEN_FOR_AUTHENTICATION_SCRIPT, projectRole);
            this.rstudioGroupService.addUserToRstudioGroup(email);
            if (!this.coderService.existsUserResearchEnvironmentWorkspace(projectCode, email)) {
                this.coderService.createWorkspace(email, projectCode);
            }
        }
    }

    private Set<ProjectBridgeheadUser> fetchActiveUsersOfDataShieldProjectsInDevelopPilotAndFinalState() {
        Set<ProjectBridgeheadUser> activeUsers = this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndProjectRole(ProjectType.DATASHIELD, ProjectState.DEVELOP, ProjectRole.DEVELOPER);
        activeUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndProjectRole(ProjectType.DATASHIELD, ProjectState.PILOT, ProjectRole.PILOT));
        activeUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndProjectRole(ProjectType.DATASHIELD, ProjectState.FINAL, ProjectRole.FINAL));
        return activeUsers;
    }

    private void sendEmail(String email, String projectCode, String bridgehead, EmailTemplateType type, ProjectRole projectRole) {
        try {
            emailService.sendEmail(email, Optional.ofNullable(projectCode), Optional.ofNullable(bridgehead), projectRole, type);
        } catch (EmailServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private void manageInactiveUsers() {
        // Manage users that are not developers in develop, pilot in pilot or final in final
        List<Mono<Object>> inactiveUsersManagement = new ArrayList<>();
        Set<ProjectEmail> usersToSendAnEmail = new HashSet<>();
        Set<ProjectBridgeheadUser> inactiveUsers = this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.DEVELOP, ProjectRole.DEVELOPER);
        inactiveUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.PILOT, ProjectRole.PILOT));
        inactiveUsers.addAll(this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.FINAL, ProjectRole.FINAL));
        inactiveUsers.stream().filter(projectBridgeheadUser -> projectBridgeheadUser.getProjectRole() != ProjectRole.CREATOR).forEach(user -> this.tokenManagerService.fetchProjectBridgeheads(
                user.getProjectBridgehead().getProject().getCode(),
                user.getProjectBridgehead().getBridgehead(),
                user.getEmail()).stream().filter(this::isBridgeheadConfiguredForTokenManager).forEach(bridgehead ->
                inactiveUsersManagement.add(manageInactiveUsers(user, bridgehead, usersToSendAnEmail))));
        // Manage active users that are not accepted in any of the bridgeheads
        fetchActiveUsersOfDataShieldProjectsInDevelopPilotAndFinalState().forEach(user ->
                this.tokenManagerService.fetchProjectBridgeheads(
                        user.getProjectBridgehead().getProject().getCode(),
                        user.getProjectBridgehead().getBridgehead(),
                        user.getEmail(),
                        projectBridgehead -> projectBridgehead.getState() != ProjectBridgeheadState.ACCEPTED).forEach(bridgehead ->
                        inactiveUsersManagement.add(manageInactiveUsers(user, bridgehead, usersToSendAnEmail))));
        Mono.when(inactiveUsersManagement).block();
    }

    private Mono<Object> manageInactiveUsers(ProjectBridgeheadUser user, String bridgehead, Set<ProjectEmail> usersToSendAnEmail) {
        // Check user status
        DataShieldTokenManagerTokenStatus dataShieldTokenManagerTokenStatus = tokenManagerService.fetchTokenStatus(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail());
        // If user token created or expired: Remove token
        if (dataShieldTokenManagerTokenStatus.tokenStatus() != DataShieldTokenStatus.NOT_FOUND) {
            Runnable ifSuccessRunnable = () -> sendEmailAndDeleteWorkspace(user.getEmail(), user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead(), user.getProjectRole(), usersToSendAnEmail);
            return tokenManagerService.removeTokens(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail(), ifSuccessRunnable);
        }
        return Mono.empty();
    }

    private void sendEmailAndDeleteWorkspace(String email, String projectCode, String bridgehead, ProjectRole projectRole, Set<ProjectEmail> usersToSendAnEmail) {
        ProjectEmail projectEmail = new ProjectEmail(email, projectCode);
        if (!usersToSendAnEmail.contains(projectEmail)) {
            usersToSendAnEmail.add(projectEmail);
            sendEmail(email, projectCode, bridgehead, EmailTemplateType.INVALID_AUTHENTICATION_SCRIPT, projectRole);
            if (projectRole != ProjectRole.FINAL || this.projectBridgeheadRepository.findByProjectCodeAndState(projectCode, ProjectBridgeheadState.ACCEPTED).isEmpty()) {
                this.rstudioGroupService.removeUserFromRstudioGroup(email);
                this.coderService.deleteWorkspace(email, projectCode);
            }
        }
    }


    private void manageInactiveProjects() {
        // Get users of DataSHIELD inactive states projects
        List<Mono<Object>> inactiveProjectsManagement = new ArrayList<>();
        List<ProjectBridgehead> inactiveProjects = this.projectBridgeheadRepository.getByProjectTypeAndNotProjectState(ProjectType.DATASHIELD, Set.of(ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL));
        Set<String> removedProjects = new HashSet<>();
        inactiveProjects.stream().filter(this::isBridgeheadConfiguredForTokenManager).forEach(projectBridgehead -> {
            Optional<ProjectBridgeheadDataShield> projectBridgeheadInDataShield = projectBridgeheadDataShieldRepository.findByProjectBridgehead(projectBridgehead);
            if (projectBridgeheadInDataShield.isEmpty() || !projectBridgeheadInDataShield.get().isRemoved()) {
                // Check user status
                DataShieldTokenManagerProjectStatus dataShieldTokenManagerProjectStatus = tokenManagerService.fetchProjectStatus(projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead());
                // if project not found: Remove Token and project
                if (dataShieldTokenManagerProjectStatus.projectStatus() != DataShieldProjectStatus.NOT_FOUND && dataShieldTokenManagerProjectStatus.projectStatus() != DataShieldProjectStatus.INACTIVE) {
                    inactiveProjectsManagement.add(tokenManagerService.removeProjectAndTokens(projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead()));
                }
            }
            setAsRemoved(projectBridgeheadInDataShield, projectBridgehead);
        });
        Mono.when(inactiveProjectsManagement).block();
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
