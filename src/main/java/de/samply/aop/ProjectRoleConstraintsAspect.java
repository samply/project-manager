package de.samply.aop;

import de.samply.annotations.RoleConstraints;
import de.samply.utils.AspectUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Aspect
public class ProjectRoleConstraintsAspect {

    private ConstraintsService constraintsService;

    public ProjectRoleConstraintsAspect(ConstraintsService constraintsService) {
        this.constraintsService = constraintsService;
    }

    @Pointcut("@annotation(de.samply.annotations.RoleConstraints)")
    public void roleConstraintsPointcut() {
    }

    @Around("roleConstraintsPointcut()")
    public Object aroundRoleConstraints(ProceedingJoinPoint joinPoint) throws Throwable {
        Optional<RoleConstraints> roleConstraints = fetchRoleConstrains(joinPoint);
        Optional<String> projectName = AspectUtils.fetchProjectName(joinPoint);
        Optional<String> bridgehead = AspectUtils.fetchBridghead(joinPoint);
        Optional<ResponseEntity> result = this.constraintsService.checkProjectRoleConstraints(roleConstraints, projectName, bridgehead);
        return (result.isEmpty()) ? joinPoint.proceed() : result;
    }

    private Optional<RoleConstraints> fetchRoleConstrains(JoinPoint joinPoint) {
        return Optional.of(AspectUtils.fetchMethod(joinPoint).getAnnotation(RoleConstraints.class));
    }


}
