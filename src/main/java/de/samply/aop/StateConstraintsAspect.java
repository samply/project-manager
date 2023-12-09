package de.samply.aop;

import de.samply.annotations.StateConstraints;
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

    @Pointcut("@annotation(de.samply.annotations.StateConstraints)")
    public void stateConstraintsPointcut() {
    }

    @Around("stateConstraintsPointcut()")
    public Object aroundStateConstraints(ProceedingJoinPoint joinPoint) throws Throwable {
        Optional<StateConstraints> stateConstraints = fetchStateConstrains(joinPoint);
        Optional<String> projectName = AspectUtils.fetchProjectName(joinPoint);
        Optional<String> bridghead = AspectUtils.fetchBridghead(joinPoint);
        Optional<ResponseEntity> result = this.constraintsService.checkStateConstraints(stateConstraints, projectName, bridghead);
        return (result.isEmpty()) ? joinPoint.proceed() : result;
    }

    private Optional<StateConstraints> fetchStateConstrains(JoinPoint joinPoint) {
        return Optional.of(AspectUtils.fetchMethod(joinPoint).getAnnotation(StateConstraints.class));
    }


}