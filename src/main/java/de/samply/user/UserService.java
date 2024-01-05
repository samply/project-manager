package de.samply.user;

import de.samply.db.model.*;
import de.samply.db.repository.*;
import de.samply.notification.NotificationService;
import de.samply.notification.OperationType;
import de.samply.project.ProjectType;
import de.samply.project.state.UserProjectState;
import de.samply.security.SessionUser;
import de.samply.token.TokenManagerService;
import de.samply.token.TokenManagerServiceException;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {

    private final NotificationService notificationService;
    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;
    private final ProjectManagerAdminUserRepository projectManagerAdminUserRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final TokenManagerService tokenManagerService;
    private final SessionUser sessionUser;

    public UserService(NotificationService notificationService,
                       BridgeheadAdminUserRepository bridgeheadAdminUserRepository,
                       ProjectManagerAdminUserRepository projectManagerAdminUserRepository,
                       ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                       ProjectRepository projectRepository,
                       ProjectBridgeheadRepository projectBridgeheadRepository,
                       TokenManagerService tokenManagerService,
                       SessionUser sessionUser) {
        this.notificationService = notificationService;
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
        this.projectManagerAdminUserRepository = projectManagerAdminUserRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.tokenManagerService = tokenManagerService;
        this.sessionUser = sessionUser;
    }

    public BridgeheadAdminUser createBridgeheadAdminUserIfNotExists(@NotNull String email, @NotNull String bridgehead) {
        Optional<BridgeheadAdminUser> bridgeheadAdminUserOptional = this.bridgeheadAdminUserRepository.findFirstByEmailAndBridgehead(email, bridgehead);
        BridgeheadAdminUser result;
        if (bridgeheadAdminUserOptional.isEmpty()) {
            result = new BridgeheadAdminUser();
            result.setBridgehead(bridgehead);
            result.setEmail(email);
            result = this.bridgeheadAdminUserRepository.save(result);
        } else {
            result = bridgeheadAdminUserOptional.get();
        }
        return result;
    }

    public ProjectManagerAdminUser createProjectManagerAdminUserIfNotExists(@NotNull String email) {
        Optional<ProjectManagerAdminUser> projectManagerAdminUserOptional = this.projectManagerAdminUserRepository.findFirstByEmail(email);
        ProjectManagerAdminUser result;
        if (projectManagerAdminUserOptional.isEmpty()) {
            result = new ProjectManagerAdminUser();
            result.setEmail(email);
            result = this.projectManagerAdminUserRepository.save(result);
        } else {
            result = projectManagerAdminUserOptional.get();
        }
        return result;
    }

    public ProjectBridgeheadUser createProjectBridgeheadUserIfNotExists(@NotNull String email, @NotNull ProjectBridgehead projectBridgehead, @NotNull ProjectRole projectRole) {
        Optional<ProjectBridgeheadUser> projectBridgeheadUserOptional = this.projectBridgeheadUserRepository.findFirstByEmailAndProjectBridgeheadAndProjectRole(email, projectBridgehead, projectRole);
        ProjectBridgeheadUser result;
        if (projectBridgeheadUserOptional.isEmpty()) {
            result = new ProjectBridgeheadUser();
            result.setEmail(email);
            result.setProjectBridgehead(projectBridgehead);
            result.setProjectRole(projectRole);
            result = this.projectBridgeheadUserRepository.save(result);
            this.notificationService.createNotification(projectBridgehead.getProject().getCode(), projectBridgehead.getBridgehead(), email, OperationType.ASSIGN_USER_TO_PROJECT,
                    "Set role " + projectRole + " to user", null, null);
        } else {
            result = projectBridgeheadUserOptional.get();
        }
        return result;
    }

    public void setProjectBridgheadUserWithRoleAndGenerateTokensIfDataShield(@NotNull String email, @NotNull String projectCode, @NotNull String bridgehead, @NotNull ProjectRole projectRole) throws UserServiceException {
        Optional<Project> project = this.projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new UserServiceException("Project " + projectCode + " not found");
        }
        Optional<ProjectBridgehead> projectBridgehead = this.projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project.get());
        if (projectBridgehead.isEmpty()) {
            throw new UserServiceException("Bridgehead " + bridgehead + " not involved in project " + projectCode);
        }
        Optional<ProjectBridgeheadUser> projectBridgeheadUserOptional = this.projectBridgeheadUserRepository.findFirstByEmailAndProjectBridgeheadAndProjectRole(email, projectBridgehead.get(), projectRole);
        if (projectBridgeheadUserOptional.isEmpty()) {
            ProjectBridgeheadUser projectBridgeheadUser = new ProjectBridgeheadUser();
            projectBridgeheadUser.setEmail(email);
            projectBridgeheadUser.setProjectRole(projectRole);
            projectBridgeheadUser.setProjectBridgehead(projectBridgehead.get());
            projectBridgeheadUser.setProjectState(UserProjectState.CREATED);
            projectBridgeheadUser.setModifiedAt(Instant.now());
            this.projectBridgeheadUserRepository.save(projectBridgeheadUser);
            this.notificationService.createNotification(projectCode, bridgehead, email, OperationType.ASSIGN_USER_TO_PROJECT,
                    "Set role " + projectRole + " to user", null, null);
        }
        if (project.get().getType() == ProjectType.DATASHIELD) {
            generateTokensAndProjectsInOpal(projectCode, bridgehead, projectBridgeheadUserOptional.get());
        }
    }

    private void generateTokensAndProjectsInOpal(@NotNull String projectCode, @NotNull String bridgehead, @NotNull ProjectBridgeheadUser projectBridgeheadUser) throws UserServiceException {
        try {
            tokenManagerService.generateTokensAndProjectsInOpal(projectCode, bridgehead,
                    () -> projectBridgeheadUserRepository.delete(projectBridgeheadUser));
        } catch (TokenManagerServiceException e) {
            throw new UserServiceException(e);
        }
    }

    public void acceptProject(@NotNull String projectCode, @NotNull String bridgehead) throws UserServiceException {
        changeProjectState(projectCode, bridgehead, UserProjectState.ACCEPTED);
    }

    public void rejectProject(@NotNull String projectCode, @NotNull String bridgehead) throws UserServiceException {
        changeProjectState(projectCode, bridgehead, UserProjectState.REJECTED);
    }

    public void requestChangesInProject(@NotNull String projectCode, @NotNull String bridgehead) throws UserServiceException {
        changeProjectState(projectCode, bridgehead, UserProjectState.REQUEST_CHANGES);
    }

    private void changeProjectState(@NotNull String projectCode, @NotNull String bridgehead, @NotNull UserProjectState state) throws UserServiceException {
        Optional<Project> project = projectRepository.findByCode(projectCode);
        if (project.isEmpty()) {
            throw new UserServiceException("Project " + projectCode + " not found");
        }
        Optional<ProjectBridgehead> projectBridgehead = projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project.get());
        if (projectBridgehead.isEmpty()) {
            throw new UserServiceException("Project " + projectCode + " for bridgehead " + bridgehead + " not found");
        }
        Optional<ProjectBridgeheadUser> projectBridgeheadUser = projectBridgeheadUserRepository.getFirstByEmailAndProjectBridgeheadOrderByModifiedAtDesc(sessionUser.getEmail(), projectBridgehead.get());
        if (projectBridgeheadUser.isEmpty()) {
            throw new UserServiceException("Project " + projectCode + " for bridgehead " + bridgehead + " and user " + sessionUser.getEmail());
        }
        projectBridgeheadUser.get().setProjectState(state);
        projectBridgeheadUser.get().setModifiedAt(Instant.now());
        projectBridgeheadUserRepository.save(projectBridgeheadUser.get());
        this.notificationService.createNotification(projectCode, bridgehead, sessionUser.getEmail(), OperationType.CHANGE_PROJECT_BRIDGEHEAD_USER_EVALUATION,
                "Set project bridgehead user evaluation to " + state, null, null);
    }

}
