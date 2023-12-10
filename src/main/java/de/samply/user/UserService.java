package de.samply.user;

import de.samply.db.model.*;
import de.samply.db.repository.*;
import de.samply.user.roles.ProjectRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;
    private final ProjectManagerAdminUserRepository projectManagerAdminUserRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;

    public UserService(BridgeheadAdminUserRepository bridgeheadAdminUserRepository,
                       ProjectManagerAdminUserRepository projectManagerAdminUserRepository,
                       ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                       ProjectRepository projectRepository,
                       ProjectBridgeheadRepository projectBridgeheadRepository) {
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
        this.projectManagerAdminUserRepository = projectManagerAdminUserRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
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

    public ProjectBridgeheadUser setProjectBridgheadUserWithRole(@NotNull String email, @NotNull String projectName, @NotNull String bridgehead, @NotNull ProjectRole projectRole) throws UserServiceException {
        Optional<Project> project = this.projectRepository.findByName(projectName);
        if (project.isEmpty()) {
            throw new UserServiceException("Project " + projectName + " not found");
        }
        Optional<ProjectBridgehead> projectBridgehead = this.projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project.get());
        if (projectBridgehead.isEmpty()) {
            throw new UserServiceException("Bridgehead " + bridgehead + " not involved in project " + projectName);
        }
        Optional<ProjectBridgeheadUser> projectBridgeheadUserOptional = this.projectBridgeheadUserRepository.findFirstByEmailAndProjectBridgeheadAndProjectRole(email, projectBridgehead.get(), projectRole);
        if (projectBridgeheadUserOptional.isEmpty()) {
            ProjectBridgeheadUser projectBridgeheadUser = new ProjectBridgeheadUser();
            projectBridgeheadUser.setEmail(email);
            projectBridgeheadUser.setProjectRole(projectRole);
            projectBridgeheadUser.setProjectBridgehead(projectBridgehead.get());
            projectBridgeheadUserOptional = Optional.of(this.projectBridgeheadUserRepository.save(projectBridgeheadUser));
        }
        return projectBridgeheadUserOptional.get();
    }

}
