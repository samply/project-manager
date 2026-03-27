package de.samply.utils;

import de.samply.annotations.*;
import de.samply.db.model.Project;
import de.samply.db.repository.ProjectRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class AspectUtils {

    public static Optional<String> fetchBridgehead(JoinPoint joinPoint) {
        return fetchParameterAnnotation(joinPoint, Bridgehead.class, String.class);
    }

    public static Optional<String> fetchProjectCode(JoinPoint joinPoint) {
        return fetchParameterAnnotation(joinPoint, ProjectCode.class, String.class);
    }

    public static Optional<String> fetchEmail(JoinPoint joinPoint) {
        return fetchParameterAnnotation(joinPoint, Email.class, String.class);
    }

    public static Optional<String> fetchMessage(JoinPoint joinPoint) {
        return fetchParameterAnnotation(joinPoint, Message.class, String.class);
    }

    public static <T> Optional<T> fetchParameterAnnotation(
            JoinPoint joinPoint,
            Class<? extends Annotation> annotationClass,
            Class<T> targetClass
    ) {
        Annotation[][] parameterAnnotations = fetchMethod(joinPoint).getParameterAnnotations();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation.annotationType() == annotationClass) {
                    if (targetClass.isInstance(args[i])) {
                        return Optional.of(targetClass.cast(args[i]));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static Method fetchMethod(JoinPoint joinPoint) {
        return ((MethodSignature) joinPoint.getSignature()).getMethod();
    }

    public static <T extends Annotation> Optional<T> fetchT(JoinPoint joinPoint, Class<T> clazz) {
        return Optional.ofNullable(fetchMethod(joinPoint).getAnnotation(clazz));
    }

    public static Optional<Project> fetchProject(ProjectRepository projectRepository, Optional<String> projectCode) {
        return (projectCode.isPresent()) ? projectRepository.findByCode(projectCode.get()) : Optional.empty();
    }

    public static String[] fetchRequestParamNames(Method method) {
        return Arrays.stream(method.getParameters())
                .flatMap(parameter -> Arrays.stream(parameter.getAnnotations())
                        .filter(annotation -> annotation instanceof RequestParam || annotation instanceof RequestVariable)
                        .map(annotation -> annotation instanceof RequestParam
                                ? ((RequestParam) annotation).name()
                                : ((RequestVariable) annotation).name()))
                .distinct()
                .toArray(String[]::new);
    }

    public static Optional<String> fetchHttpMethod(Method method) {
        RequestMapping annotation = method.getAnnotation(RequestMapping.class);
        if (annotation == null) {
            for (Annotation methodAnnotation : method.getDeclaredAnnotations()) {
                for (Annotation methodAnnotationAnnotation : methodAnnotation.annotationType().getDeclaredAnnotations()) {
                    if (methodAnnotationAnnotation.annotationType() == RequestMapping.class) {
                        return fetchHttpMethod((RequestMapping) methodAnnotationAnnotation);
                    }
                }
            }
        }
        assert annotation != null;
        return fetchHttpMethod(annotation);
    }

    private static Optional<String> fetchHttpMethod(RequestMapping requestMapping) {
        return Optional.of(requestMapping.method()[0].name());
    }

}
