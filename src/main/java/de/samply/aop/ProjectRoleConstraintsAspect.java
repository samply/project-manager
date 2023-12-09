package de.samply.aop;

import de.samply.annotations.RoleConstraints;
import de.samply.db.model.Project;
import de.samply.db.repository.ProjectRepository;
import de.samply.user.roles.OrganisationRoleToProjectRoleMapper;
import de.samply.user.roles.ProjectRole;
import de.samply.user.roles.UserProjectRoles;
import de.samply.utils.AspectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Aspect
public class ProjectRoleConstraintsAspect {

    private final ProjectRepository projectRepository;
    private final OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper;

    public ProjectRoleConstraintsAspect(ProjectRepository projectRepository, OrganisationRoleToProjectRoleMapper organisationRoleToProjectRoleMapper) {
        this.projectRepository = projectRepository;
        this.organisationRoleToProjectRoleMapper = organisationRoleToProjectRoleMapper;
    }

    @Pointcut("@annotation(de.samply.annotations.RoleConstraints)")
    public void roleConstraintsPointcut() {
    }

    @Around("roleConstraintsPointcut()")
    public Object aroundRoleConstraints(ProceedingJoinPoint joinPoint) throws Throwable {
        RoleConstraints roleConstraints = fetchRoleConstrains(joinPoint);
        if (roleConstraints.projectRoles().length > 0) {
            Optional<String> projectName = AspectUtils.fetchProjectName(joinPoint);
            if (projectName.isEmpty() || projectName.get().length() == 0) {
                return ResponseEntity.badRequest().body("Project name not provided");
            }
            Optional<Project> project = AspectUtils.fetchProject(projectRepository, projectName);
            if (project.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            boolean userHasProjectRoleInProject = false;
            for (ProjectRole projectRole : roleConstraints.projectRoles()) {
                if (userHasProjectRoleInProject(project.get(), projectRole, AspectUtils.fetchBridghead(joinPoint))) {
                    userHasProjectRoleInProject = true;
                    break;
                }
            }
            if (!userHasProjectRoleInProject) {
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
            }
        }
        return joinPoint.proceed();
    }

    private boolean userHasProjectRoleInProject(Project project, ProjectRole projectRole, Optional<String> bridgehead) {
        Optional<UserProjectRoles> projectRoles = organisationRoleToProjectRoleMapper.map(project);
        return (projectRoles.isPresent()) ? projectRoles.get().containsRole(projectRole, bridgehead) : false;
    }

    private RoleConstraints fetchRoleConstrains(JoinPoint joinPoint) {
        return AspectUtils.fetchMethod(joinPoint).getAnnotation(RoleConstraints.class);
    }


}
