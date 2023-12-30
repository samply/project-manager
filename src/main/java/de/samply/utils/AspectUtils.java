package de.samply.utils;

import de.samply.annotations.Bridgehead;
import de.samply.annotations.Email;
import de.samply.annotations.ProjectCode;
import de.samply.db.model.Project;
import de.samply.db.repository.ProjectRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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

    public static String[] fetchRequestParamNames(Method method) {
        Set<String> result = new HashSet<>();
        Arrays.stream(method.getParameters()).forEach(parameter -> Arrays.stream(parameter.getAnnotations())
                .filter(annotation -> annotation.annotationType() == RequestParam.class)
                .map(annotation -> (RequestParam) annotation)
                .forEach(requestParam -> result.add(requestParam.name())));
        return result.toArray(String[]::new);
    }

    public static Optional<String> fetchHttpMethod(Method method) {
        RequestMapping annotation = method.getAnnotation(RequestMapping.class);
        if (annotation == null) {
            for (Annotation methodAnnotation : method.getDeclaredAnnotations()) {
                for (Annotation methodAnnotationAnnotation : methodAnnotation.annotationType().getDeclaredAnnotations()) {
                    if (methodAnnotationAnnotation.annotationType() == RequestMapping.class) {
                        return fetchHttpMetod((RequestMapping) methodAnnotationAnnotation);
                    }
                }
            }
        }
        return fetchHttpMetod(annotation);
    }

    private static Optional<String> fetchHttpMetod(RequestMapping requestMapping) {
        return Optional.of(requestMapping.method()[0].name());
    }

}
