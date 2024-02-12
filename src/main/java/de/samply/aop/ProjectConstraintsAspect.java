package de.samply.aop;

import de.samply.annotations.ProjectConstraints;
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
public class ProjectConstraintsAspect {

    private final ConstraintsService constraintsService;

    public ProjectConstraintsAspect(ConstraintsService constraintsService) {
        this.constraintsService = constraintsService;
    }

    @Pointcut("@annotation(de.samply.annotations.ProjectConstraints)")
    public void projectConstraintsPointcut() {
    }

    @Around("projectConstraintsPointcut()")
    public Object aroundProjectConstraints(ProceedingJoinPoint joinPoint) throws Throwable {
        Optional<ProjectConstraints> projectConstraints = fetchProjectConstrains(joinPoint);
        Optional<String> projectCode = AspectUtils.fetchProjectCode(joinPoint);
        Optional<ResponseEntity> result = this.constraintsService.checkProjectConstraints(projectConstraints, projectCode);
        return (result.isEmpty()) ? joinPoint.proceed() : result.get();
    }

    private Optional<ProjectConstraints> fetchProjectConstrains(JoinPoint joinPoint) {
        return AspectUtils.fetchT(joinPoint, ProjectConstraints.class);
    }

}
