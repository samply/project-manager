package de.samply.user;

import de.samply.db.model.*;
import de.samply.db.repository.*;
import de.samply.project.state.UserProjectState;
import de.samply.security.SessionUser;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {

    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;
    private final ProjectManagerAdminUserRepository projectManagerAdminUserRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final SessionUser sessionUser;

    public UserService(BridgeheadAdminUserRepository bridgeheadAdminUserRepository,
                       ProjectManagerAdminUserRepository projectManagerAdminUserRepository,
                       ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                       ProjectRepository projectRepository,
                       ProjectBridgeheadRepository projectBridgeheadRepository,
                       SessionUser sessionUser) {
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
        this.projectManagerAdminUserRepository = projectManagerAdminUserRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
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
        } else {
            result = projectBridgeheadUserOptional.get();
        }
        return result;
    }

    public void setProjectBridgheadUserWithRole(@NotNull String email, @NotNull String projectCode, @NotNull String bridgehead, @NotNull ProjectRole projectRole) throws UserServiceException {
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
    }

}
