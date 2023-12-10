package de.samply.user.roles;

import de.samply.db.model.Project;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.security.SessionUser;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class OrganisationRoleToProjectRoleMapper {

    private final SessionUser sessionUser;
    private final ProjectRepository projectRepository;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private Map<OrganisationRole, ProjectRole> organisationToProjectRoleMap = Map.of(
            OrganisationRole.PROJECT_MANAGER_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN,
            OrganisationRole.BRIDGEHEAD_ADMIN, ProjectRole.BRIDGEHEAD_ADMIN
    );

    public OrganisationRoleToProjectRoleMapper(SessionUser sessionUser,
                                               ProjectRepository projectRepository,
                                               ProjectBridgeheadRepository projectBridgeheadRepository,
                                               ProjectBridgeheadUserRepository projectBridgeheadUserRepository) {
        this.sessionUser = sessionUser;
        this.projectRepository = projectRepository;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
    }

    public Optional<UserProjectRoles> map(String projectName) {
        if (projectName == null) {
            return Optional.empty();
        }
        Optional<Project> project = projectRepository.findByName(projectName);
        return (project.isPresent()) ? map(project.get()) : Optional.empty();
    }

    public Optional<UserProjectRoles> map(Project project) {
        UserProjectRoles result = new UserProjectRoles();
        if (project == null || sessionUser.getUserOrganisationRoles() == null) {
            return Optional.empty();
        }
        sessionUser.getUserOrganisationRoles().getRolesNotDependentOnBridgeheads().forEach(organisationRole -> {
            ProjectRole projectRole = organisationToProjectRoleMap.get(organisationRole);
            if (projectRole != null) {
                result.addRoleNotDependentOnBridgehead(projectRole);
            }
        });
        if (project.getCreatorEmail().equals(sessionUser.getEmail())) {
            result.addRoleNotDependentOnBridgehead(ProjectRole.CREATOR);
        }
        sessionUser.getUserOrganisationRoles().getBridgeheads().forEach(bridgehead ->
                sessionUser.getUserOrganisationRoles().getBridgeheadRoles(bridgehead).get().forEach(organisationRole -> {
                    ProjectRole projectRole = organisationToProjectRoleMap.get(organisationRole);
                    if (projectRole != null) {
                        result.addBridgeheadRole(bridgehead, projectRole);
                    } else {
                        projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead, project).ifPresent(projectBridgehead ->
                                projectBridgeheadUserRepository.getByEmailAndProjectBridgehead(sessionUser.getEmail(), projectBridgehead).forEach(projectBridgeheadUser ->
                                        result.addBridgeheadRole(bridgehead, projectBridgeheadUser.getProjectRole())));
                    }
                }));
        return Optional.of(result);
    }

}
