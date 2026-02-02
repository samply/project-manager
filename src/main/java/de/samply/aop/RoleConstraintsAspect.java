package de.samply.aop;

import de.samply.annotations.RoleConstraints;
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
public class RoleConstraintsAspect {

    private final ConstraintsService constraintsService;

    public RoleConstraintsAspect(ConstraintsService constraintsService) {
        this.constraintsService = constraintsService;
    }

    @Pointcut("@annotation(de.samply.annotations.RoleConstraints)")
    public void roleConstraintsPointcut() {
    }

    @SuppressWarnings("rawtypes") // For Optional<ResponseEntity>. Otherwise, it would be too complex
    @Around("roleConstraintsPointcut()")
    public Object aroundRoleConstraints(ProceedingJoinPoint joinPoint) throws Throwable {
        Optional<RoleConstraints> roleConstraints = fetchRoleConstrains(joinPoint);
        Optional<StateConstraints> stateConstraints = fetchStateConstrains(joinPoint);
        Optional<String> projectCode = AspectUtils.fetchProjectCode(joinPoint);
        Optional<String> bridgehead = AspectUtils.fetchBridgehead(joinPoint);
        Optional<ResponseEntity> result = this.constraintsService.checkRoleConstraints(roleConstraints, stateConstraints, projectCode, bridgehead);
        return (result.isEmpty()) ? joinPoint.proceed() : result.get();
    }

    private Optional<RoleConstraints> fetchRoleConstrains(JoinPoint joinPoint) {
        return AspectUtils.fetchT(joinPoint, RoleConstraints.class);
    }

    private Optional<StateConstraints> fetchStateConstrains(JoinPoint joinPoint) {
        return AspectUtils.fetchT(joinPoint, StateConstraints.class);
    }


}
