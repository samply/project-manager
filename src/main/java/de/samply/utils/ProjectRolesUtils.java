package de.samply.utils;

import de.samply.user.roles.ProjectRole;

import java.util.*;
import java.util.function.Function;

public class ProjectRolesUtils {

    // O(1) lookup map for role order (descending in time)
    private static final Map<ProjectRole, Integer> bridgeheadProjectRoleOrder =
            Map.of(
                    ProjectRole.FINAL, 0,
                    ProjectRole.PILOT, 1,
                    ProjectRole.DEVELOPER, 2,
                    ProjectRole.CREATOR, 3,
                    ProjectRole.BRIDGEHEAD_ADMIN, 4,
                    ProjectRole.PROJECT_MANAGER_ADMIN, 5
            );

    // Compare two ProjectRoles by their defined order
    public static int compare(ProjectRole r1, ProjectRole r2) {
        return Integer.compare(
                bridgeheadProjectRoleOrder.getOrDefault(r1, Integer.MAX_VALUE),
                bridgeheadProjectRoleOrder.getOrDefault(r2, Integer.MAX_VALUE)
        );
    }

    // Order a collection of ProjectRoles
    public static List<ProjectRole> orderCollectionInDescendentTime(Collection<ProjectRole> projectRoleSet) {
        return orderCollectionInDescendentTime(projectRoleSet, projectRole -> projectRole);
    }

    // Generic ordering utility based on a mapping function
    public static <T> List<T> orderCollectionInDescendentTime(Collection<T> tCollection,
                                                              Function<T, ProjectRole> projectRoleFunction) {
        List<T> result = new ArrayList<>(tCollection);
        result.sort(Comparator.comparingInt(t -> bridgeheadProjectRoleOrder
                .getOrDefault(projectRoleFunction.apply(t), Integer.MAX_VALUE)));
        return result;
    }

}
