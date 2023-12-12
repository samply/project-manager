package de.samply.aop;

import de.samply.annotations.NotEmpty;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@Component
@Aspect
public class NotEmptyAspect {

    @Pointcut("@annotation(de.samply.annotations.NotEmpty)")
    public void notEmptyPointcut() {
    }

    @Around("notEmptyPointcut()")
    public Object validateParameterNotEmpty(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs(); // Get method arguments
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();
        AtomicReference<String> emptyParameter = new AtomicReference<>("");
        IntStream.range(0, args.length)
                .filter(i -> hasNotEmptyAnnotation(parameterAnnotations[i]))
                .filter(i -> args[i] == null || StringUtils.isEmpty(args[i].toString().trim()))
                .findFirst()
                .ifPresent(i -> emptyParameter.set(signature.getParameterNames()[i]));
        return (!emptyParameter.get().isEmpty()) ?
                ResponseEntity.badRequest().body("Parameter" + " cannot be empty") : joinPoint.proceed();
    }

    private boolean hasNotEmptyAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(NotEmpty.class)) {
                return true;
            }
        }
        return false;
    }
}
