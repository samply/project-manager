package de.samply.project;

import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.query.QueryState;
import de.samply.user.roles.ProjectRole;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProjectBridgeheadUserService {

    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;

    public ProjectBridgeheadUserService(ProjectBridgeheadUserRepository projectBridgeheadUserRepository) {
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
    }

    public List<Project> fetchProjects(String email) {
        return projectBridgeheadUserRepository.findProjectsByEmail(email);
    }

    public List<ProjectBridgeheadUser> fetchUsers(String email, ProjectBridgehead projectBridgehead) {
        return projectBridgeheadUserRepository.getByEmailAndProjectBridgehead(email, projectBridgehead);
    }

    public Optional<ProjectBridgeheadUser> fetchFirstUsersOrderByModifiedAtDesc(String email, ProjectBridgehead projectBridgehead) {
        return projectBridgeheadUserRepository.getFirstByEmailAndProjectBridgeheadOrderByModifiedAtDesc(email, projectBridgehead);
    }

    public Set<ProjectBridgeheadUser> fetchUsers(ProjectRole projectRole, Project project) {
        return projectBridgeheadUserRepository.getDistinctByProjectRoleAndProjectCode(projectRole, project.getCode());
    }

    public Optional<ProjectBridgeheadUser> fetchFirstValidUser(String email, ProjectBridgehead projectBridgehead) {
        return projectBridgeheadUserRepository.getFirstValidByEmailAndProjectBridgehead(email, projectBridgehead);
    }

    public Set<ProjectBridgeheadUser> fetchUsersWithNotProjectRole(ProjectType projectType, ProjectState projectState, ProjectRole projectRole) {
        return projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndNotProjectRole(projectType, projectState, projectRole);
    }

    public Set<ProjectBridgeheadUser> fetchUsers(ProjectType projectType, ProjectState projectState, ProjectRole projectRole) {
        return projectBridgeheadUserRepository.getByProjectTypeAndProjectStateAndProjectRole(projectType, projectState, projectRole);
    }

    public List<ProjectBridgeheadUser> fetchUsersInValidProjectState(ProjectType projectType, QueryState queryState, ProjectBridgeheadState projectBridgeheadState) {
        return projectBridgeheadUserRepository.getDistinctInValidProjectStateByProjectTypeAndQueryStateAndProjectBridgeheadState(projectType, queryState, projectBridgeheadState);
    }

    public List<ProjectBridgeheadUser> fetchUsersInInvalidProjectState(ProjectType projectType) {
        return projectBridgeheadUserRepository.getDistinctInInvalidProjectStateByProjectType(projectType);
    }

    public List<ProjectBridgeheadUser> fetchUsers(ProjectBridgehead bridgehead) {
        return projectBridgeheadUserRepository.getByProjectBridgehead(bridgehead);
    }

    public Optional<ProjectBridgeheadUser> fetchFirstUser(String email, ProjectBridgehead projectBridgehead, ProjectRole projectRole) {
        return projectBridgeheadUserRepository.findFirstByEmailAndProjectBridgeheadAndProjectRole(email, projectBridgehead, projectRole);
    }

    public List<ProjectBridgeheadUser> fetchDistinctUsers(String email, ProjectBridgehead projectBridgehead){
        return projectBridgeheadUserRepository.getDistinctByEmailContainingAndProjectBridgehead(email, projectBridgehead);
    }

    public List<ProjectBridgeheadUser> fetchUsers(ProjectRole role, ProjectBridgehead projectBridgehead){
        return projectBridgeheadUserRepository.getDistinctByProjectRoleAndProjectBridgehead(role, projectBridgehead);
    }

    public List<ProjectBridgeheadUser> fetchUsersAlreadySetInThisProjectForThisRole(String email, ProjectBridgehead bridgehead){
        return projectBridgeheadUserRepository.getDistinctByEmailContainingAndProjectBridgehead_BridgeheadAndUserAlreadySetForThisProjectInThisRole(email, bridgehead.getBridgehead(), bridgehead.getProject().getCode());
    }

    public void saveUser(ProjectBridgeheadUser user) {
        user.setModifiedAt(Instant.now());
        projectBridgeheadUserRepository.save(user);
    }


}
