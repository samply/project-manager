package de.samply.aop;

import de.samply.annotations.Bridgehead;
import de.samply.annotations.ProjectName;
import de.samply.annotations.RoleConstraints;
import de.samply.db.model.Project;
import de.samply.db.repository.ProjectRepository;
import de.samply.user.roles.OrganisationRoleToProjectRoleMapper;
import de.samply.user.roles.ProjectRole;
import de.samply.user.roles.UserProjectRoles;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

@Component
@Slf4j
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
            Optional<String> projectName = fetchProjectName(joinPoint);
            if (projectName.isEmpty() || projectName.get().length() == 0) {
                return ResponseEntity.badRequest().body("Project name not provided");
            }
            Optional<Project> project = fetchProject(projectName);
            if (project.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            boolean userHasProjectRoleInProject = true;
            for (ProjectRole projectRole : roleConstraints.projectRoles()) {
                if (userHasProjectRoleInProject(project.get(), projectRole, fetchBridghead(joinPoint))) {
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

    private Optional<Project> fetchProject(Optional<String> projectName) {
        return (projectName.isPresent()) ? projectRepository.findByName(projectName.get()) : Optional.empty();
    }

    private Optional<String> fetchBridghead(JoinPoint joinPoint) {
        return fetchStringParamterAnnotation(joinPoint, Bridgehead.class);
    }

    private Optional<String> fetchProjectName(JoinPoint joinPoint) {
        return fetchStringParamterAnnotation(joinPoint, ProjectName.class);
    }

    private Optional<String> fetchStringParamterAnnotation(JoinPoint joinPoint, Class annotationClass) {
        Annotation[][] parameterAnnotations = fetchMethod(joinPoint).getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation.annotationType() == annotationClass) {
                    if (args[i] instanceof String) {
                        return Optional.ofNullable((String) args[i]);
                    }
                }
            }
        }
        return Optional.empty();
    }


    private RoleConstraints fetchRoleConstrains(JoinPoint joinPoint) {
        return fetchMethod(joinPoint).getAnnotation(RoleConstraints.class);
    }

    private Method fetchMethod(JoinPoint joinPoint) {
        return ((MethodSignature) joinPoint.getSignature()).getMethod();
    }

}
