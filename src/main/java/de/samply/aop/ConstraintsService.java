package de.samply.aop;

import de.samply.annotations.RoleConstraints;
import de.samply.annotations.StateConstraints;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.OrganisationRoleToProjectRoleMapper;
import de.samply.user.roles.ProjectRole;
import de.samply.user.roles.UserProjectRoles;
import de.samply.utils.AspectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConstraintsService {

    private final ProjectRepository projectRepository;
    private final OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final SessionUser sessionUser;

    public ConstraintsService(ProjectRepository projectRepository,
                              OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper,
                              ProjectBridgeheadRepository projectBridgeheadRepository,
                              SessionUser sessionUser) {
        this.projectRepository = projectRepository;
        this.organisationRoleToProjectRoleMapper = organisationRoleToProjectRoleMapper;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.sessionUser = sessionUser;
    }

    public Optional<ResponseEntity> checkRoleConstraints(Optional<RoleConstraints> roleConstraints, Optional<String> projectCode, Optional<String> bridgehead) {
        Optional<ResponseEntity> result = checkOrganisationRoleConstraints(roleConstraints, bridgehead);
        return (result.isPresent()) ? result : checkProjectRoleConstraints(roleConstraints, projectCode, bridgehead);
    }

    public Optional<ResponseEntity> checkOrganisationRoleConstraints(Optional<RoleConstraints> roleConstraints, Optional<String> bridgehead) {
        if (roleConstraints.isPresent() && roleConstraints.get().organisationRoles().length > 0) {
            boolean hasAnyOrganisationRole = false;
            for (OrganisationRole organisationRole : roleConstraints.get().organisationRoles()) {
                if (sessionUser.getUserOrganisationRoles().containsRole(organisationRole, bridgehead)) {
                    hasAnyOrganisationRole = true;
                    break;
                }
            }
            if (!hasAnyOrganisationRole) {
                return Optional.of(ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build());
            }
        }
        return Optional.empty();
    }

    public Optional<ResponseEntity> checkProjectRoleConstraints(Optional<RoleConstraints> roleConstraints, Optional<String> projectCode, Optional<String> bridgehead) {
        if (roleConstraints.isPresent() && roleConstraints.get().projectRoles().length > 0) {
            if (projectCode.isEmpty() || projectCode.get().length() == 0) {
                return Optional.of(ResponseEntity.badRequest().body("Project code not provided"));
            }
            Optional<Project> project = AspectUtils.fetchProject(projectRepository, projectCode);
            if (project.isEmpty()) {
                return Optional.of(ResponseEntity.notFound().build());
            }
            boolean userHasProjectRoleInProject = false;
            for (ProjectRole projectRole : roleConstraints.get().projectRoles()) {
                if (userHasProjectRoleInProject(project.get(), projectRole, bridgehead)) {
                    userHasProjectRoleInProject = true;
                    break;
                }
            }
            if (!userHasProjectRoleInProject) {
                return Optional.of(ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build());
            }
        }
        return Optional.empty();
    }

    private boolean userHasProjectRoleInProject(Project project, ProjectRole projectRole, Optional<String> bridgehead) {
        Optional<UserProjectRoles> projectRoles = organisationRoleToProjectRoleMapper.map(project);
        return (projectRoles.isPresent()) ? projectRoles.get().containsRole(projectRole, bridgehead) : false;
    }

    public Optional<ResponseEntity> checkStateConstraints(Optional<StateConstraints> stateConstraints, Optional<String> projectCode, Optional<String> bridgehead) {
        if (stateConstraints.isPresent()) {
            if (projectCode.isEmpty() || projectCode.get().length() == 0) {
                return Optional.of(ResponseEntity.badRequest().body("Project code not provided"));
            }
            Optional<Project> project = AspectUtils.fetchProject(projectRepository, projectCode);
            if (project.isEmpty()) {
                return Optional.of(ResponseEntity.notFound().build());
            }
            if (stateConstraints.get().projectStates().length > 0) {
                boolean hasAnyProjectStateConstraint = false;
                for (ProjectState projectState : stateConstraints.get().projectStates()) {
                    if (project.get().getState() == projectState) {
                        hasAnyProjectStateConstraint = true;
                        break;
                    }
                }
                if (!hasAnyProjectStateConstraint) {
                    return Optional.of(ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build());
                }
            }
            if (stateConstraints.get().projectBridgeheadStates().length > 0) {
                Optional<ProjectBridgehead> projectBridgehead = fetchProjectBridgehead(project.get(), bridgehead);
                if (projectBridgehead.isEmpty()) {
                    return Optional.of(ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build());
                }
                boolean hasAnyProjectBridgeheadStateConstraint = false;
                for (ProjectBridgeheadState projectBridgeheadState : stateConstraints.get().projectBridgeheadStates()) {
                    if (projectBridgehead.get().getState() == projectBridgeheadState) {
                        hasAnyProjectBridgeheadStateConstraint = true;
                        break;
                    }
                }
                if (!hasAnyProjectBridgeheadStateConstraint) {
                    return Optional.of(ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<ProjectBridgehead> fetchProjectBridgehead(Project project, Optional<String> bridgehead) {
        return (bridgehead.isEmpty()) ? Optional.empty() : this.projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead.get(), project);
    }

}
