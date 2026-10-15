package de.samply.aop;

import de.samply.annotations.RequiresModule;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Refuses a call to an endpoint of a disabled optional module ({@link RequiresModule}), also as an entry of an actions
 * batch.
 */
@Component
@Aspect
public class RequiresModuleAspect {

    private final ConstraintsService constraintsService;

    public RequiresModuleAspect(ConstraintsService constraintsService) {
        this.constraintsService = constraintsService;
    }

    @SuppressWarnings("EmptyMethod")
    @Pointcut("@annotation(de.samply.annotations.RequiresModule)")
    public void requiresModulePointcut() {
    }

    @Around("requiresModulePointcut()")
    public Object aroundRequiresModule(ProceedingJoinPoint joinPoint) throws Throwable {
        Optional<RequiresModule> requiresModule = Optional.ofNullable(
                ((MethodSignature) joinPoint.getSignature()).getMethod().getAnnotation(RequiresModule.class));
        @SuppressWarnings("rawtypes") Optional<ResponseEntity> result = constraintsService.checkModuleConstraints(requiresModule);
        return result.isEmpty() ? joinPoint.proceed() : result.get();
    }

}
