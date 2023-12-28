package de.samply.utils;

import de.samply.annotations.Bridgehead;
import de.samply.annotations.Email;
import de.samply.annotations.ProjectCode;
import de.samply.db.model.Project;
import de.samply.db.repository.ProjectRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

public class AspectUtils {

    public static Optional<String> fetchBridghead(JoinPoint joinPoint) {
        return fetchStringParameterAnnotation(joinPoint, Bridgehead.class);
    }

    public static Optional<String> fetchProjectCode(JoinPoint joinPoint) {
        return fetchStringParameterAnnotation(joinPoint, ProjectCode.class);
    }

    public static Optional<String> fetchEmail(JoinPoint joinPoint) {
        return fetchStringParameterAnnotation(joinPoint, Email.class);
    }

    private static Optional<String> fetchStringParameterAnnotation(JoinPoint joinPoint, Class annotationClass) {
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

    public static Method fetchMethod(JoinPoint joinPoint) {
        return ((MethodSignature) joinPoint.getSignature()).getMethod();
    }

    public static <T extends Annotation> Optional<T> fetchT(JoinPoint joinPoint, Class<T> clazz) {
        return Optional.of(fetchMethod(joinPoint).getAnnotation(clazz));
    }

    public static Optional<Project> fetchProject(ProjectRepository projectRepository, Optional<String> projectCode) {
        return (projectCode.isPresent()) ? projectRepository.findByCode(projectCode.get()) : Optional.empty();
    }


}
