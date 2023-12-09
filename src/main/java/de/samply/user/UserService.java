package de.samply.user;

import de.samply.db.model.BridgeheadAdminUser;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.model.ProjectManagerAdminUser;
import de.samply.db.repository.BridgeheadAdminUserRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectManagerAdminUserRepository;
import de.samply.user.roles.ProjectRole;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final BridgeheadAdminUserRepository bridgeheadAdminUserRepository;
    private final ProjectManagerAdminUserRepository projectManagerAdminUserRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;

    public UserService(BridgeheadAdminUserRepository bridgeheadAdminUserRepository,
                       ProjectManagerAdminUserRepository projectManagerAdminUserRepository,
                       ProjectBridgeheadUserRepository projectBridgeheadUserRepository) {
        this.bridgeheadAdminUserRepository = bridgeheadAdminUserRepository;
        this.projectManagerAdminUserRepository = projectManagerAdminUserRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
    }

    public BridgeheadAdminUser createBridgeheadAdminUserIfNotExists(String email, String bridgehead) {
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

    public ProjectManagerAdminUser createProjectManagerAdminUserIfNotExists(String email) {
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

    public ProjectBridgeheadUser createProjectBridgeheadUserIfNotExists(String email, ProjectBridgehead projectBridgehead, ProjectRole projectRole) {
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

}
