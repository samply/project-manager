package de.samply.utils;

import de.samply.annotations.Bridgehead;
import de.samply.annotations.ProjectName;
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

    public static Optional<String> fetchProjectName(JoinPoint joinPoint) {
        return fetchStringParameterAnnotation(joinPoint, ProjectName.class);
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

    public static Optional<Project> fetchProject(ProjectRepository projectRepository, Optional<String> projectName) {
        return (projectName.isPresent()) ? projectRepository.findByName(projectName.get()) : Optional.empty();
    }


}
