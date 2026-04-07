package de.samply.user.roles;

import de.samply.db.model.Project;
import de.samply.project.ProjectBridgeheadService;
import de.samply.project.ProjectBridgeheadUserService;
import de.samply.security.SessionUser;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class OrganisationRoleToProjectRoleMapper {

    private final SessionUser sessionUser;

    // Services
    private final ProjectBridgeheadService projectBridgeheadService;
    private final ProjectBridgeheadUserService projectBridgeheadUserService;

    private final Map<OrganisationRole, ProjectRole> organisationToProjectRoleMap = Map.of(
            OrganisationRole.PROJECT_MANAGER_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN,
            OrganisationRole.BRIDGEHEAD_ADMIN, ProjectRole.BRIDGEHEAD_ADMIN
    );

    public OrganisationRoleToProjectRoleMapper(SessionUser sessionUser,
                                               ProjectBridgeheadService projectBridgeheadService,
                                               ProjectBridgeheadUserService projectBridgeheadUserService) {
        this.sessionUser = sessionUser;
        this.projectBridgeheadService = projectBridgeheadService;
        this.projectBridgeheadUserService = projectBridgeheadUserService;
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
                sessionUser.getUserOrganisationRoles().getBridgeheadRoles(bridgehead).forEach(organisationRole -> {
                    ProjectRole projectRole = organisationToProjectRoleMap.get(organisationRole);
                    if (projectRole != null) {
                        result.addBridgeheadRole(bridgehead, projectRole);
                    } else {
                        projectBridgeheadService
                                .fetchBridgehead(project, bridgehead)
                                .ifPresent(projectBridgehead ->
                                        projectBridgeheadUserService.fetchUsers(
                                                        sessionUser.getEmail(),
                                                        projectBridgehead)
                                                .forEach(projectBridgeheadUser ->
                                                        result.addBridgeheadRole(
                                                                bridgehead,
                                                                projectBridgeheadUser.getProjectRole())));
                    }
                }));
        return Optional.of(result);
    }

}
