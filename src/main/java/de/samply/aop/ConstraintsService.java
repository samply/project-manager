package de.samply.aop;

import de.samply.annotations.ProjectConstraints;
import de.samply.annotations.RoleConstraints;
import de.samply.annotations.StateConstraints;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
import de.samply.db.model.ProjectBridgeheadUser;
import de.samply.db.repository.ProjectBridgeheadRepository;
import de.samply.db.repository.ProjectBridgeheadUserRepository;
import de.samply.db.repository.ProjectRepository;
import de.samply.project.state.ProjectBridgeheadState;
import de.samply.project.state.ProjectState;
import de.samply.project.state.UserProjectState;
import de.samply.security.SessionUser;
import de.samply.user.roles.OrganisationRole;
import de.samply.user.roles.OrganisationRoleToProjectRoleMapper;
import de.samply.user.roles.ProjectRole;
import de.samply.user.roles.UserProjectRoles;
import de.samply.utils.AspectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("rawtypes") // For Optional<ResponseEntity>. Otherwise, it would be too complex
@Service
public class ConstraintsService {

    private final ProjectRepository projectRepository;
    private final OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper;
    private final ProjectBridgeheadRepository projectBridgeheadRepository;
    private final ProjectBridgeheadUserRepository projectBridgeheadUserRepository;
    private final SessionUser sessionUser;

    private final Map<ProjectRole, ProjectState> temporalProjectRoleProjectStateMap = Map.of(
            ProjectRole.DEVELOPER, ProjectState.DEVELOP,
            ProjectRole.PILOT, ProjectState.PILOT,
            ProjectRole.FINAL, ProjectState.FINAL);

    public ConstraintsService(ProjectRepository projectRepository,
                              OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper,
                              ProjectBridgeheadRepository projectBridgeheadRepository,
                              ProjectBridgeheadUserRepository projectBridgeheadUserRepository,
                              SessionUser sessionUser) {
        this.projectRepository = projectRepository;
        this.organisationRoleToProjectRoleMapper = organisationRoleToProjectRoleMapper;
        this.projectBridgeheadRepository = projectBridgeheadRepository;
        this.projectBridgeheadUserRepository = projectBridgeheadUserRepository;
        this.sessionUser = sessionUser;
    }

    public Optional<ResponseEntity> checkRoleConstraints(Optional<RoleConstraints> roleConstraints, Optional<StateConstraints> stateConstraints, Optional<String> projectCode, Optional<String> bridgehead) {
        Optional<ResponseEntity> result = checkOrganisationRoleConstraints(roleConstraints);
        return (result.isPresent()) ? result : checkProjectRoleConstraints(roleConstraints, stateConstraints, projectCode, bridgehead);
    }

    public Optional<ResponseEntity> checkOrganisationRoleConstraints(Optional<RoleConstraints> roleConstraints) {
        if (roleConstraints.isPresent() && roleConstraints.get().organisationRoles().length > 0) {
            boolean hasAnyOrganisationRole = false;
            for (OrganisationRole organisationRole : roleConstraints.get().organisationRoles()) {
                if (sessionUser.getUserOrganisationRoles().containsAnyRole(organisationRole)) {
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

    public Optional<ResponseEntity> checkProjectRoleConstraints(Optional<RoleConstraints> roleConstraints, Optional<StateConstraints> stateConstraints, Optional<String> projectCode, Optional<String> bridgehead) {
        if (roleConstraints.isPresent() && roleConstraints.get().projectRoles().length > 0) {
            if (notContainsProjectCode(projectCode)) {
                return Optional.of(ResponseEntity.badRequest().body("Project code not provided"));
            }
            Optional<Project> project = AspectUtils.fetchProject(projectRepository, projectCode);
            if (project.isEmpty()) {
                return Optional.of(ResponseEntity.notFound().build());
            }
            Optional<UserProjectRoles> userProjectRoles = organisationRoleToProjectRoleMapper.map(project.get());
            boolean userHasProjectRoleInProject = false;
            for (ProjectRole projectRole : roleConstraints.get().projectRoles()) {
                if (userHasProjectRoleInProject(userProjectRoles, projectRole, bridgehead) &&
                        isProjectRoleInAuthorizedProjectState(projectRole, project.get(), stateConstraints)) {
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

    @SuppressWarnings("OptionalAssignedToNull") // Referred to projectCode == null
    private boolean notContainsProjectCode(Optional<String> projectCode) {
        return projectCode == null || projectCode.isEmpty();
    }

    private boolean isProjectRoleInAuthorizedProjectState(ProjectRole projectRole, Project project, Optional<StateConstraints> stateConstraints) {
        ProjectState projectState = temporalProjectRoleProjectStateMap.get(projectRole);
        if (projectState == null || stateConstraints.isEmpty() || stateConstraints.get().projectStates().length == 0) {
            return true;
        }
        return project.getState() == projectState && Arrays.asList(stateConstraints.get().projectStates()).contains(projectState);
    }

    private boolean userHasProjectRoleInProject(Optional<UserProjectRoles> userProjectRoles, ProjectRole projectRole, Optional<String> bridgehead) {
        return userProjectRoles.isPresent() && userProjectRoles.get().containsRole(projectRole, bridgehead);
    }

    public Optional<ResponseEntity> checkStateConstraints(Optional<StateConstraints> stateConstraints, Optional<String> projectCode, Optional<String> bridgehead) {
        if (stateConstraints.isPresent()) {
            if (notContainsProjectCode(projectCode)) {
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
            if (stateConstraints.get().projectBridgeheadStates().length > 0 || stateConstraints.get().queryStates().length > 0 || stateConstraints.get().userProjectStates().length > 0) {
                Optional<ProjectBridgehead> projectBridgehead = fetchProjectBridgehead(project.get(), bridgehead);
                if (projectBridgehead.isEmpty()) {
                    return Optional.of(ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build());
                }
                boolean hasAnyProjectBridgeheadStateConstraint = true;
                if (stateConstraints.get().projectBridgeheadStates().length > 0) {
                    hasAnyProjectBridgeheadStateConstraint = false;
                    for (ProjectBridgeheadState projectBridgeheadState : stateConstraints.get().projectBridgeheadStates()) {
                        if (projectBridgehead.get().getState() == projectBridgeheadState) {
                            hasAnyProjectBridgeheadStateConstraint = true;
                            break;
                        }
                    }
                }
                if (hasAnyProjectBridgeheadStateConstraint && stateConstraints.get().queryStates().length > 0) {
                    hasAnyProjectBridgeheadStateConstraint = projectBridgehead.get().getExecutions().stream()
                            .anyMatch(exec -> Arrays.asList(stateConstraints.get().queryStates())
                                    .contains(exec.getQueryState()));
                }
                if (hasAnyProjectBridgeheadStateConstraint && stateConstraints.get().userProjectStates().length > 0) {
                    hasAnyProjectBridgeheadStateConstraint = false;
                    Optional<ProjectBridgeheadUser> projectBridgeheadUser = fetchProjectBridgeheadUser(projectBridgehead.get());
                    if (projectBridgeheadUser.isPresent()) {
                        for (UserProjectState userProjectState : stateConstraints.get().userProjectStates()) {
                            if (projectBridgeheadUser.get().getProjectState() == userProjectState) {
                                hasAnyProjectBridgeheadStateConstraint = true;
                                break;
                            }
                        }
                    }
                    if (!hasAnyProjectBridgeheadStateConstraint) {
                        Optional<UserProjectRoles> userRoles = organisationRoleToProjectRoleMapper.map(project.get());
                        if (userRoles.isPresent() && userRoles.get().containsRole(ProjectRole.CREATOR)) {
                            for (UserProjectState userProjectState : stateConstraints.get().userProjectStates()) {
                                if (project.get().getCreatorResultsState() == userProjectState) {
                                    hasAnyProjectBridgeheadStateConstraint = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!hasAnyProjectBridgeheadStateConstraint) {
                    return Optional.of(ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build());
                }
            }
        }
        return Optional.empty();
    }

    public Optional<ResponseEntity> checkProjectConstraints(Optional<ProjectConstraints> projectConstraints, Optional<String> projectCode) {
        //TODO
        if (projectConstraints.isPresent()) {
            if (notContainsProjectCode(projectCode)) {
                return Optional.of(ResponseEntity.badRequest().body("Project code not provided"));
            }
            Optional<Project> project = AspectUtils.fetchProject(projectRepository, projectCode);
            if (project.isEmpty()) {
                return Optional.of(ResponseEntity.notFound().build());
            }
            if (projectConstraints.get().projectTypes().length > 0) {
                boolean hasAnyProjectTypeConstraint = Arrays.stream(projectConstraints.get().projectTypes())
                        .anyMatch(pc -> project.get().hasProjectType(pc));

                if (!hasAnyProjectTypeConstraint) {
                    return Optional.of(ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<ProjectBridgehead> fetchProjectBridgehead(Project project, Optional<String> bridgehead) {
        return (bridgehead.isEmpty()) ? Optional.empty() : this.projectBridgeheadRepository.findFirstByBridgeheadAndProject(bridgehead.get(), project);
    }

    private Optional<ProjectBridgeheadUser> fetchProjectBridgeheadUser(ProjectBridgehead projectBridgehead) {
        return this.projectBridgeheadUserRepository.getFirstByEmailAndProjectBridgeheadOrderByModifiedAtDesc(sessionUser.getEmail(), projectBridgehead);
    }

}
