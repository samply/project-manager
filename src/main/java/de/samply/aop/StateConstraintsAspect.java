package de.samply.aop;

import de.samply.annotations.StateConstraints;
import de.samply.db.model.Project;
import de.samply.db.model.ProjectBridgehead;
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
public class StateConstraintsAspect {

    private final ConstraintsService constraintsService;

    public StateConstraintsAspect(ConstraintsService constraintsService) {
        this.constraintsService = constraintsService;
    }

    @SuppressWarnings("EmptyMethod")
    @Pointcut("@annotation(de.samply.annotations.StateConstraints)")
    public void stateConstraintsPointcut() {
    }

    @Around("stateConstraintsPointcut()")
    public Object aroundStateConstraints(ProceedingJoinPoint joinPoint) throws Throwable {
        Optional<StateConstraints> stateConstraints = fetchStateConstraints(joinPoint);
        Optional<Project> project = AspectUtils.fetchProject(joinPoint);
        Optional<ProjectBridgehead> bridgehead = AspectUtils.fetchBridgehead(joinPoint);
        @SuppressWarnings("rawtypes") Optional<ResponseEntity> result = this.constraintsService.checkStateConstraints(stateConstraints, project, bridgehead);
        return (result.isEmpty()) ? joinPoint.proceed() : result.get();
    }

    private Optional<StateConstraints> fetchStateConstraints(JoinPoint joinPoint) {
        return AspectUtils.fetchT(joinPoint, StateConstraints.class);
    }


}
