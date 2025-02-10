package de.samply.datashield;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.coder.CoderService;
import de.samply.datashield.dto.DataShieldProjectStatus;
import de.samply.datashield.dto.DataShieldTokenStatus;
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
import de.samply.register.AppRegisterService;
import de.samply.rstudio.group.RstudioGroupService;
import de.samply.user.roles.ProjectRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Slf4j
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
    private final AppRegisterService appRegisterService;
    private final boolean isTokenManagerActive;

    public DataShieldTokenManagerJob(RstudioGroupService rstudioGroupService,
                                     CoderService coderService,
                                     DataShieldTokenManagerService tokenManagerService,
                                     ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                                     ProjectBridgeheadRepository projectBridgeheadRepository,
                                     ProjectBridgeheadDataShieldRepository projectBridgeheadDataShieldRepository,
                                     EmailService emailService, BridgeheadConfiguration bridgeheadConfiguration,
                                     AppRegisterService appRegisterService,
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
        this.appRegisterService = appRegisterService;
        this.isTokenManagerActive = isTokenManagerActive;
    }

    @Scheduled(cron = ProjectManagerConst.MANAGE_TOKENS_CRON_EXPRESSION_SV)
    public void manageTokens() {
        if (isTokenManagerActive) {
            log.debug("Starting DataSHIELD Job...");
            Mono.when(
                    manageActiveUsers(),
                    manageInactiveUsers(),
                    manageInactiveProjects()).block();
            log.debug("DataSHIELD Job finished");
        }
    }

    private Mono<Void> manageActiveUsers() {
        log.debug("Manage DataSHIELD active users...");
        Set<ProjectEmail> usersToSendAnEmail = new HashSet<>();
        return Flux.fromIterable(
                        fetchActiveUsersOfDataShieldProjectsInDevelopPilotAndFinalState().stream()
                                .flatMap(user ->
                                        tokenManagerService.fetchProjectBridgeheads(
                                                        user.getProjectBridgehead().getProject().getCode(),
                                                        user.getProjectBridgehead().getBridgehead(),
                                                        user.getEmail(),
                                                        projectBridgehead -> projectBridgehead.getState() == ProjectBridgeheadState.ACCEPTED).stream()
                                                .filter(this::isBridgeheadConfiguredForTokenManager)
                                                .map(bridgehead -> new UserBridgehead(user, bridgehead)))
                                .toList())
                .flatMap(userBridgehead ->
                        tokenManagerService.fetchTokenStatus(userBridgehead.user().getProjectBridgehead().getProject().getCode(),
                                        userBridgehead.bridgehead, userBridgehead.user().getEmail())
                                .filter(status -> status.projectStatus() == DataShieldProjectStatus.WITH_DATA)
                                .flatMap(status -> {
                                    Supplier<Mono> ifSuccessMonoSupplier = () ->
                                            sendNewTokenEmailAndCreateWorkspaceIfNotExists(userBridgehead.user().getEmail(),
                                                    userBridgehead.user().getProjectBridgehead().getProject().getCode(),
                                                    userBridgehead.bridgehead(),
                                                    userBridgehead.user().getProjectRole(), usersToSendAnEmail);
                                    if (status.tokenStatus() == DataShieldTokenStatus.NOT_FOUND) { // If user token not found: Create token
                                        return tokenManagerService.generateTokensInOpal(
                                                userBridgehead.user().getProjectBridgehead().getProject().getCode(),
                                                userBridgehead.bridgehead(), userBridgehead.user().getEmail(), ifSuccessMonoSupplier);
                                    } else if (status.tokenStatus() == DataShieldTokenStatus.EXPIRED) { // If user token expired: Refresh Token
                                        return tokenManagerService.refreshToken(
                                                userBridgehead.user().getProjectBridgehead().getProject().getCode(),
                                                userBridgehead.bridgehead(),
                                                userBridgehead.user().getEmail(), ifSuccessMonoSupplier);
                                    } else {
                                        return Mono.empty();
                                    }
                                }))
                .then();
    }

    private record UserBridgehead(ProjectBridgeheadUser user, String bridgehead) {
    }

    private Mono<Void> sendNewTokenEmailAndCreateWorkspaceIfNotExists(String email, String projectCode, String bridgehead, ProjectRole projectRole, Set<ProjectEmail> usersToSendAnEmail) {
        ProjectEmail projectEmail = new ProjectEmail(projectCode, bridgehead);
        if (!usersToSendAnEmail.contains(projectEmail)) {
            usersToSendAnEmail.add(projectEmail);
            sendEmail(email, projectCode, bridgehead, EmailTemplateType.NEW_TOKEN_FOR_AUTHENTICATION_SCRIPT, projectRole);
            this.rstudioGroupService.addUserToRstudioGroup(email);
            if (!this.coderService.existsUserResearchEnvironmentWorkspace(projectCode, bridgehead, email)) {
                return this.coderService.createWorkspace(email, projectCode).flatMap(appRegisterService::register);
            }
        }
        return Mono.empty();
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

    private Mono<Void> manageInactiveUsers() {
        log.debug("Manage DataSHIELD inactive users...");
        Set<ProjectEmail> usersToSendAnEmail = new HashSet<>();
        return Flux.fromIterable(Stream.concat(
                        // Manage users that are not developers in develop, pilot in pilot or final in final
                        Stream.of(
                                        this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.DEVELOP, ProjectRole.DEVELOPER),
                                        this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.PILOT, ProjectRole.PILOT),
                                        this.projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(ProjectType.DATASHIELD, ProjectState.FINAL, ProjectRole.FINAL))
                                .flatMap(Set::stream)
                                .filter(user -> user.getProjectRole() != ProjectRole.CREATOR)
                                .flatMap(user ->
                                        this.tokenManagerService.fetchProjectBridgeheads(
                                                        user.getProjectBridgehead().getProject().getCode(),
                                                        user.getProjectBridgehead().getBridgehead(),
                                                        user.getEmail()).stream()
                                                .filter(this::isBridgeheadConfiguredForTokenManager)
                                                .map(bridgehead -> new UserBridgehead(user, bridgehead))),

                        // Manage active users that are not accepted in any of the bridgeheads
                        fetchActiveUsersOfDataShieldProjectsInDevelopPilotAndFinalState().stream()
                                .flatMap(user -> this.tokenManagerService.fetchProjectBridgeheads(
                                                user.getProjectBridgehead().getProject().getCode(),
                                                user.getProjectBridgehead().getBridgehead(),
                                                user.getEmail(),
                                                projectBridgehead -> projectBridgehead.getState() != ProjectBridgeheadState.ACCEPTED).stream()
                                        .filter(this::isBridgeheadConfiguredForTokenManager)
                                        .map(bridgehead -> new UserBridgehead(user, bridgehead)))).toList())
                .flatMap(userBridgehead -> manageInactiveUsers(userBridgehead.user(), userBridgehead.bridgehead(), usersToSendAnEmail))
                .then();
    }

    private Mono<Void> manageInactiveUsers(ProjectBridgeheadUser user, String bridgehead, Set<ProjectEmail> usersToSendAnEmail) {
        return tokenManagerService.fetchTokenStatus(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail()) // Check user status
                .filter(status -> status.tokenStatus() != DataShieldTokenStatus.NOT_FOUND && status.tokenStatus() != DataShieldTokenStatus.INACTIVE)
                .flatMap(status ->
                        // If user token created or expired: Remove token
                        tokenManagerService.removeTokens(user.getProjectBridgehead().getProject().getCode(), bridgehead, user.getEmail(),
                                () -> sendEmailAndDeleteWorkspace(user.getEmail(), user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead(), user.getProjectRole(), usersToSendAnEmail)));
    }

    private Mono<Void> sendEmailAndDeleteWorkspace(String email, String projectCode, String bridgehead, ProjectRole projectRole, Set<ProjectEmail> usersToSendAnEmail) {
        ProjectEmail projectEmail = new ProjectEmail(email, projectCode);
        if (!usersToSendAnEmail.contains(projectEmail)) {
            usersToSendAnEmail.add(projectEmail);
            sendEmail(email, projectCode, bridgehead, EmailTemplateType.INVALID_AUTHENTICATION_SCRIPT, projectRole);
            if (projectRole != ProjectRole.FINAL || this.projectBridgeheadRepository.findByProjectCodeAndState(projectCode, ProjectBridgeheadState.ACCEPTED).isEmpty()) {
                this.rstudioGroupService.removeUserFromRstudioGroup(email);
                return this.coderService.deleteWorkspace(email, projectCode).flatMap(appRegisterService::unregister);
            }
        }
        return Mono.empty();
    }


    private boolean isBridgeheadConfiguredForTokenManager(ProjectBridgehead projectBridgehead) {
        return isBridgeheadConfiguredForTokenManager(projectBridgehead.getBridgehead());
    }

    private boolean isBridgeheadConfiguredForTokenManager(String bridgehead) {
        return this.bridgeheadConfiguration.getTokenManagerId(bridgehead).isPresent();
    }

    private Mono<Void> manageInactiveProjects() {
        log.debug("Manage DataSHIELD active projects...");
        // Get users of DataSHIELD inactive states projects
        return Flux.fromIterable(this.projectBridgeheadRepository.getByProjectTypeAndNotProjectState(ProjectType.DATASHIELD, Set.of(ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL)).stream()
                        .filter(this::isBridgeheadConfiguredForTokenManager)
                        .filter(projectBridgehead -> {
                            Optional<ProjectBridgeheadDataShield> projectBridgeheadDataShield = projectBridgeheadDataShieldRepository.findByProjectBridgehead(projectBridgehead);
                            setAsRemoved(projectBridgeheadDataShield, projectBridgehead);
                            return projectBridgeheadDataShield.isEmpty() || !projectBridgeheadDataShield.get().isRemoved();
                        }).toList())
                .flatMap(projectBridgeheadDataShield -> tokenManagerService.fetchProjectStatus(projectBridgeheadDataShield.getProject().getCode(), projectBridgeheadDataShield.getBridgehead()))
                .filter(dataShieldTokenManagerProjectStatus ->
                        dataShieldTokenManagerProjectStatus.projectStatus() != DataShieldProjectStatus.NOT_FOUND &&
                                dataShieldTokenManagerProjectStatus.projectStatus() != DataShieldProjectStatus.INACTIVE)
                .flatMap(dataShieldTokenManagerProjectStatus ->
                        tokenManagerService.removeProjectAndTokens(dataShieldTokenManagerProjectStatus.projectCode(), dataShieldTokenManagerProjectStatus.bridgehead()))
                .then();
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
