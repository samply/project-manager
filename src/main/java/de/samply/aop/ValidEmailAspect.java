package de.samply.aop;

import de.samply.annotations.Email;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

// It checks if the parameters marked with the annotation @Email of a REST service, are valid emails.
// If not, it returns BAD REQUEST
@Component
@Aspect
public class ValidEmailAspect {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    @Pointcut("execution(* *(.., @de.samply.annotations.Email (*), ..))")
    public void emailPointcut() {
    }

    @Around("emailPointcut()")
    public Object validateParameterNotEmpty(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs(); // Get method arguments
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();
        AtomicReference<Optional<String>> invalidEmail = new AtomicReference<>(Optional.empty());
        IntStream.range(0, args.length)
                .filter(i -> hasEmailAnnotation(parameterAnnotations[i]))
                .filter(i -> !isValidEmail(args[i].toString().trim()))
                .findFirst()
                .ifPresent(i -> invalidEmail.set(Optional.of(args[i].toString().trim())));
        return (invalidEmail.get().isPresent()) ?
                ResponseEntity.badRequest().body("Invalid email: " + invalidEmail.get().get()) : joinPoint.proceed();
    }

    private boolean hasEmailAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Email.class)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidEmail(String email){
        if (StringUtils.hasText(email)){
            return EMAIL_PATTERN.matcher(email).matches();
        }
        return true; // Ignore null parameters
    }


}
