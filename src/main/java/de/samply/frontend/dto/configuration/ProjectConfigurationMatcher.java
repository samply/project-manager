package de.samply.frontend.dto.configuration;

import de.samply.frontend.dto.Project;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectConfigurationMatcher {

    public static Map<String, Project> fetchMatchProjectConfiguration(Project project, Map<String, Project> projectConfigurations) throws ProjectConfigurationMatcherException {
        Map<Map.Entry<String, Project>, Integer> matchedFieldCounts = new HashMap<>();
        for (Map.Entry<String, Project> entry : projectConfigurations.entrySet()) {
            int matchedFields = countMatchedFields(project, entry.getValue());
            if (matchedFields > 0) {
                matchedFieldCounts.put(entry, matchedFields);
            }
        }
        Optional<Map.Entry<String, Project>> nameProjectConfig = matchedFieldCounts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey);
        return (nameProjectConfig.isPresent()) ? Map.ofEntries(nameProjectConfig.get()) : new HashMap<>();
    }

    private static int countMatchedFields(Project project, Project projectConfiguration) {
        try {
            return countMatchedFieldsWithoutHandlingException(project, projectConfiguration);
        } catch (IllegalAccessException e) {
            throw new ProjectConfigurationMatcherException(e);
        }
    }

    private static int countMatchedFieldsWithoutHandlingException(Project project, Project projectConfiguration) throws IllegalAccessException {
        AtomicInteger matchedFields = new AtomicInteger(0);
        for (Field field : project.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object projectValue = field.get(project);
            Object templateValue = field.get(projectConfiguration);
            if (templateValue != null) {
                if (templateValue.equals(projectValue)) {
                    matchedFields.getAndIncrement();
                } else {
                    return -1;
                }
            }
        }
        return matchedFields.get();
    }

}
