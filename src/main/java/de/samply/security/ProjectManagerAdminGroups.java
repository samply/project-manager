package de.samply.security;

import de.samply.app.ProjectManagerConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ProjectManagerAdminGroups {

    private final List<String> groups;

    public ProjectManagerAdminGroups(@Value(ProjectManagerConst.PM_ADMIN_GROUPS_SV) List<String> groups) {
        this.groups = groups.stream().map(GroupNames::normalize).filter(group -> !group.isEmpty()).toList();
    }

    public boolean contains(String group) {
        return this.groups.contains(GroupNames.normalize(group));
    }

}
