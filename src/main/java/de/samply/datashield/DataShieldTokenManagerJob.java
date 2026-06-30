package de.samply.datashield;

import de.samply.app.ProjectManagerConst;
import de.samply.bridgehead.BridgeheadConfiguration;
import de.samply.coder.CoderService;
import de.samply.datashield.dto.DataShieldProjectStatus;
import de.samply.datashield.dto.DataShieldTokenManagerProjectStatus;
import de.samply.datashield.dto.DataShieldTokenStatus;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadDataShield;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.email.EmailService;
import de.samply.email.EmailServiceException;
import de.samply.email.EmailTemplateType;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.project.ProjectType;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.register.AppRegisterService;
import de.samply.user.roles.ProjectRole;
import lombok.Data;
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

    // Services
    private final CoderService coderService;
    private final ProjectBridgeheadService projectBridgeheadService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;
    private final DataShieldTokenManagerService tokenManagerService;
    private final EmailService emailService;
    private final AppRegisterService appRegisterService;

    private final BridgeheadConfiguration bridgeheadConfiguration;

    private final boolean isTokenManagerActive;

    public DataShieldTokenManagerJob(CoderService coderService,
                                     ProjectBridgeheadService projectBridgeheadService,
                                     ProjectBridgeheadUserService projectBridgeheadUserService,
                                     DataShieldTokenManagerService tokenManagerService,
                                     EmailService emailService,
                                     BridgeheadConfiguration bridgeheadConfiguration,
                                     AppRegisterService appRegisterService,
                                     @Value(ProjectManagerConst.ENABLE_TOKEN_MANAGER_SV) boolean isTokenManagerActive
    ) {
        this.coderService = coderService;
        this.projectBridgeheadService = projectBridgeheadService;
        this.projectBridgeheadUserService = projectBridgeheadUserService;
        this.tokenManagerService = tokenManagerService;
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
                                                        user.getProjectBridgehead().getProject(),
                                                        user.getProjectBridgehead(),
                                                        user.getEmail(),
                                                        projectBridgehead -> projectBridgehead.getState() == ProjectBridgeheadState.ACCEPTED).stream()
                                                .filter(this::isBridgeheadConfiguredForTokenManager)
                                                .map(bridgehead -> new UserBridgehead(user, bridgehead)))
                                .toList())
                .flatMap(userBridgehead ->
                        tokenManagerService.fetchTokenStatus(userBridgehead.user().getProjectBridgehead().getProject(),
                                        userBridgehead.bridgehead, userBridgehead.user().getEmail())
                                .filter(status -> status.projectStatus() == DataShieldProjectStatus.WITH_DATA)
                                .flatMap(status -> {
                                    Supplier<Mono> ifSuccessMonoSupplier = () ->
                                            sendNewTokenEmailAndCreateWorkspaceIfNotExists(userBridgehead.user(), usersToSendAnEmail);
                                    if (status.tokenStatus() == DataShieldTokenStatus.NOT_FOUND) { // If user token not found: Create token
                                        return tokenManagerService.generateTokensInOpal(
                                                userBridgehead.user().getProjectBridgehead().getProject(),
                                                userBridgehead.bridgehead(), userBridgehead.user().getEmail(), ifSuccessMonoSupplier);
                                    } else if (status.tokenStatus() == DataShieldTokenStatus.EXPIRED) { // If user token expired: Refresh Token
                                        return tokenManagerService.refreshToken(
                                                userBridgehead.user().getProjectBridgehead().getProject(),
                                                userBridgehead.bridgehead(),
                                                userBridgehead.user().getEmail(), ifSuccessMonoSupplier);
                                    } else if (status.tokenStatus() == DataShieldTokenStatus.CREATED) {
                                        return createWorkspaceIfNotExists(userBridgehead.user());
                                    } else {
                                        return Mono.empty();
                                    }
                                }))
                .then();
    }

    private record UserBridgehead(ProjectBridgeheadUser user, ProjectBridgehead bridgehead) {
    }

    private Mono<Void> sendNewTokenEmailAndCreateWorkspaceIfNotExists(ProjectBridgeheadUser user, Set<ProjectEmail> usersToSendAnEmail) {
        ProjectEmail projectEmail = new ProjectEmail(user.getProjectBridgehead().getProject().getCode(), user.getProjectBridgehead().getBridgehead());
        if (!usersToSendAnEmail.contains(projectEmail)) {
            usersToSendAnEmail.add(projectEmail);
            sendEmail(user.getEmail(), user.getProjectBridgehead().getProject(), user.getProjectBridgehead(), EmailTemplateType.NEW_TOKEN_FOR_AUTHENTICATION_SCRIPT, user.getProjectRole());
            return createWorkspaceIfNotExists(user);
        }
        return Mono.empty();
    }

    private Mono<Void> createWorkspaceIfNotExists(ProjectBridgeheadUser user) {
        return !this.coderService.existsUserResearchEnvironmentWorkspace(user) ?
                this.coderService.createWorkspace(user).flatMap(appRegisterService::register) : Mono.empty();
    }

    private Set<ProjectBridgeheadUser> fetchActiveUsersOfDataShieldProjectsInDevelopPilotAndFinalState() {
        Set<ProjectBridgeheadUser> activeUsers = projectBridgeheadUserService.fetchUsers(ProjectType.DATASHIELD, ProjectState.DEVELOP, ProjectRole.DEVELOPER);
        activeUsers.addAll(projectBridgeheadUserService.fetchUsers(ProjectType.DATASHIELD, ProjectState.PILOT, ProjectRole.PILOT));
        activeUsers.addAll(projectBridgeheadUserService.fetchUsers(ProjectType.DATASHIELD, ProjectState.FINAL, ProjectRole.FINAL));
        return activeUsers;
    }

    private void sendEmail(String email, Project project, ProjectBridgehead bridgehead, EmailTemplateType type, ProjectRole projectRole) {
        try {
            emailService.sendEmail(email, Optional.ofNullable(project), Optional.ofNullable(bridgehead), projectRole, type);
        } catch (EmailServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private Mono<Void> manageInactiveUsers() {
        log.debug("Manage DataSHIELD inactive users...");
        Set<ProjectEmail> usersToSendAnEmail = new HashSet<>();
        return Flux.fromIterable(Stream.concat(
                        // Manage users that are not developers in development, pilot in pilot or final in final
                        Stream.of(
                                        projectBridgeheadUserService.fetchUsersWithNotProjectRole(ProjectType.DATASHIELD, ProjectState.DEVELOP, ProjectRole.DEVELOPER),
                                        projectBridgeheadUserService.fetchUsersWithNotProjectRole(ProjectType.DATASHIELD, ProjectState.PILOT, ProjectRole.PILOT),
                                        projectBridgeheadUserService.fetchUsersWithNotProjectRole(ProjectType.DATASHIELD, ProjectState.FINAL, ProjectRole.FINAL))
                                .flatMap(Set::stream)
                                .flatMap(user ->
                                        this.tokenManagerService.fetchProjectBridgeheads(
                                                        user.getProjectBridgehead().getProject(),
                                                        user.getProjectBridgehead(),
                                                        user.getEmail()).stream()
                                                .filter(this::isBridgeheadConfiguredForTokenManager)
                                                .map(bridgehead -> new UserBridgehead(user, bridgehead))),

                        // Manage active users that are not accepted in any of the bridgeheads
                        fetchActiveUsersOfDataShieldProjectsInDevelopPilotAndFinalState().stream()
                                .flatMap(user -> this.tokenManagerService.fetchProjectBridgeheads(
                                                user.getProjectBridgehead().getProject(),
                                                user.getProjectBridgehead(),
                                                user.getEmail(),
                                                projectBridgehead -> projectBridgehead.getState() != ProjectBridgeheadState.ACCEPTED).stream()
                                        .filter(this::isBridgeheadConfiguredForTokenManager)
                                        .map(bridgehead -> new UserBridgehead(user, bridgehead)))).toList())
                .flatMap(userBridgehead -> manageInactiveUsers(userBridgehead.user(), userBridgehead.bridgehead(), usersToSendAnEmail))
                .then();
    }

    private Mono<Void> manageInactiveUsers(ProjectBridgeheadUser user, ProjectBridgehead bridgehead, Set<ProjectEmail> usersToSendAnEmail) {
        return tokenManagerService.fetchTokenStatus(user.getProjectBridgehead().getProject(), bridgehead, user.getEmail()) // Check user status
                .filter(status -> status.tokenStatus() != DataShieldTokenStatus.NOT_FOUND && status.tokenStatus() != DataShieldTokenStatus.INACTIVE)
                .flatMap(_ -> {
                    // If a user token created or expired: Remove token
                    if (projectBridgeheadUserService.fetchFirstValidUser(user.getEmail(), user.getProjectBridgehead()).isEmpty()) { // Check that is not valid again (e.g., user that is a developer and final user in the final state
                        return tokenManagerService.removeTokens(user.getProjectBridgehead().getProject(), bridgehead, user.getEmail(),
                                () -> sendEmailAndDeleteWorkspace(user, usersToSendAnEmail));
                    } else {
                        return deleteWorkspace(user);
                    }
                });
    }

    private Mono<Void> sendEmailAndDeleteWorkspace(ProjectBridgeheadUser user, Set<ProjectEmail> usersToSendAnEmail) {
        ProjectEmail projectEmail = new ProjectEmail(
                user.getEmail(),
                user.getProjectBridgehead().getProject().getCode());
        if (!usersToSendAnEmail.contains(projectEmail)) {
            usersToSendAnEmail.add(projectEmail);
            sendEmail(
                    user.getEmail(),
                    user.getProjectBridgehead().getProject(),
                    user.getProjectBridgehead(),
                    EmailTemplateType.INVALID_AUTHENTICATION_SCRIPT,
                    user.getProjectRole());
            if (user.getProjectRole() != ProjectRole.FINAL ||
                    projectBridgeheadService.fetchBridgeheads(
                                    user.getProjectBridgehead().getProject(),
                                    ProjectBridgeheadState.ACCEPTED)
                            .isEmpty()) {
                return deleteWorkspace(user);
            }
        }
        return Mono.empty();
    }

    private Mono<Void> deleteWorkspace(ProjectBridgeheadUser user) {
        return this.coderService.deleteWorkspace(user).flatMap(appRegisterService::unregister);
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
        return Flux.fromIterable(
                        projectBridgeheadService
                                .fetchByProjectTypeAndNotProjectState(
                                        ProjectType.DATASHIELD,
                                        Set.of(ProjectState.DEVELOP, ProjectState.PILOT, ProjectState.FINAL))
                                .stream()
                                .filter(this::isBridgeheadConfiguredForTokenManager)
                                .map(projectBridgehead ->
                                        new InactiveProject(projectBridgeheadService.fetchDataShield(projectBridgehead), projectBridgehead))
                                .filter(inactiveProject -> inactiveProject.getProjectBridgeheadDataShield().isEmpty() || !inactiveProject.getProjectBridgeheadDataShield().get().isRemoved())
                                .toList())
                .flatMap(inactiveProject ->
                        tokenManagerService.fetchProjectStatus(inactiveProject.getProjectBridgehead().getProject(), inactiveProject.getProjectBridgehead())
                                .flatMap(status -> {
                                    inactiveProject.setStatus(status);
                                    return Mono.just(inactiveProject);
                                })
                )
                .flatMap(inactiveProject ->
                        Mono.when(
                                        Mono.just(inactiveProject)
                                                .filter(_ ->
                                                        inactiveProject.getStatus().projectStatus() != DataShieldProjectStatus.NOT_FOUND &&
                                                                inactiveProject.getStatus().projectStatus() != DataShieldProjectStatus.INACTIVE)
                                                .flatMap(_ -> tokenManagerService.removeProjectAndTokens(inactiveProject.getProjectBridgehead().getProject(), inactiveProject.getProjectBridgehead())),
                                        coderService.deleteAllWorkspaces(inactiveProject.getProjectBridgehead().getProject().getCode(), inactiveProject.getProjectBridgehead().getBridgehead())
                                                .flatMap(appRegisterService::unregister).then())
                                .doOnSuccess(_ -> setAsRemovedIfConditions(inactiveProject))
                )
                .then();
    }

    @Data
    private static class InactiveProject {
        private Optional<ProjectBridgeheadDataShield> projectBridgeheadDataShield;
        private ProjectBridgehead projectBridgehead;
        private DataShieldTokenManagerProjectStatus status;

        public InactiveProject(Optional<ProjectBridgeheadDataShield> projectBridgeheadDataShield, ProjectBridgehead projectBridgehead) {
            this.projectBridgeheadDataShield = projectBridgeheadDataShield;
            this.projectBridgehead = projectBridgehead;
        }

    }

    private void setAsRemovedIfConditions(InactiveProject inactiveProject) {
        ProjectBridgeheadDataShield result = null;
        if (inactiveProject.getProjectBridgeheadDataShield().isPresent()) {
            if ((inactiveProject.getStatus().projectStatus() == DataShieldProjectStatus.NOT_FOUND ||
                    inactiveProject.getStatus().projectStatus() == DataShieldProjectStatus.INACTIVE) &&
                    coderService.existsUserResearchEnvironmentWorkspace(
                            inactiveProject.getProjectBridgehead().getProject(),
                            inactiveProject.getProjectBridgehead())
            ) {
                result = inactiveProject.getProjectBridgeheadDataShield().get();
                result.setRemoved(true);
            }
        } else {
            result = new ProjectBridgeheadDataShield();
            result.setProjectBridgehead(inactiveProject.getProjectBridgehead());
        }
        if (result != null) {
            projectBridgeheadService.saveDataShield(result);
        }
    }

}
