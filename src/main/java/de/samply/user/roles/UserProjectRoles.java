package de.samply.user.roles;

import de.samply.utils.ProjectRolesUtils;

import java.util.List;

public class UserProjectRoles extends UserRoles<ProjectRole> {

    public List<ProjectRole> getBridgeheadRolesOrderedInDescendentTime(String bridgehead) {
        return ProjectRolesUtils.orderCollectionInDescendentTime(getBridgeheadRoles(bridgehead));
    }

}
