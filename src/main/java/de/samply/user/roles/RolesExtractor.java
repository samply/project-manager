package de.samply.user.roles;

import de.samply.annotations.RoleConstraints;
import de.samply.app.ProjectManagerController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RolesExtractor {

    public static Map<String, String[]> extractPathRolesMap() {
        Map<String, String[]> result = new HashMap<>();
        String rootPath = getRootPath();
        Arrays.stream(ProjectManagerController.class.getDeclaredMethods()).forEach(method -> {
            RoleConstraints roleConstraints = method.getAnnotation(RoleConstraints.class);
            if (roleConstraints != null && roleConstraints.organisationRoles().length > 0) {
                fetchPath(method).ifPresent(path -> result.put(rootPath + path,
                        Arrays.stream(roleConstraints.organisationRoles()).map(role -> role.name()).toArray(String[]::new)));
            }
        });
        return result;
    }

    private static Optional<String> fetchPath(Method method) {
        RequestMapping annotation = method.getAnnotation(RequestMapping.class);
        if (annotation == null) {
            for (Annotation methodAnnotation : method.getDeclaredAnnotations()) {
                for (Annotation methodAnnotationAnnotation : methodAnnotation.annotationType().getDeclaredAnnotations()) {
                    if (methodAnnotationAnnotation.annotationType() == RequestMapping.class) {
                        return fetchPath(methodAnnotation);
                    }
                }
            }
            return Optional.empty();
        } else {
            return fetchPath(annotation);
        }
    }


    private static Optional<String> fetchPath(Annotation annotation) {
        try {
            return (annotation != null) ? fetchPathWithoutExceptionHandling(annotation) : Optional.empty();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> fetchPathWithoutExceptionHandling(Annotation annotation) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String[] value = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
        String[] path = (String[]) annotation.annotationType().getMethod("path").invoke(annotation);
        return Optional.ofNullable((value != null && value.length > 0) ? value[0] : (path != null && path.length > 0) ? path[0] : null);
    }

    private static String getRootPath() {
        Optional<String> path = fetchPath(ProjectManagerController.class.getAnnotation(RequestMapping.class));
        return (path.isPresent()) ? path.get() : "";
    }

}
