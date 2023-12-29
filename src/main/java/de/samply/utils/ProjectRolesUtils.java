package de.samply.utils;

import de.samply.user.roles.ProjectRole;

import java.util.*;
import java.util.function.Function;

public class ProjectRolesUtils {

    private static List<ProjectRole> bridgeheadProjectRolesOrderedInTimeDescendent = List.of(ProjectRole.FINAL, ProjectRole.PILOT, ProjectRole.DEVELOPER, ProjectRole.CREATOR, ProjectRole.BRIDGEHEAD_ADMIN, ProjectRole.PROJECT_MANAGER_ADMIN);

    public static List<ProjectRole> orderCollectionInDescendentTime(Collection<ProjectRole> projectRoleSet) {
        return orderCollectionInDescendentTime(projectRoleSet, projectRole -> projectRole);
    }

    public static <T> List<T> orderCollectionInDescendentTime(Collection<T> tCollection, Function<T, ProjectRole> projectRoleFunction) {
        List<T> result = new ArrayList<>(tCollection);
        Collections.sort(result, Comparator.comparingInt(t -> bridgeheadProjectRolesOrderedInTimeDescendent.indexOf(projectRoleFunction.apply(t))));
        return result;
    }

    public static int compare(ProjectRole projectRole1, ProjectRole projectRole2) {
        return Comparator.comparingInt(projectRole -> bridgeheadProjectRolesOrderedInTimeDescendent.indexOf(projectRole)).compare(projectRole1, projectRole2);
    }

}
