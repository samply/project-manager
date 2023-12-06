package de.samply.security;

import de.samply.app.ProjectManagerConst;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = ProjectManagerConst.PM_ADMIN_GROUP_PROPERTY)
public class ProjectManagerAdminGroups {

    private Map<String, String> groups = new HashMap<>();

    public void setGroups(Map<String, String> groups) {
        this.groups = groups;
    }

    public boolean contains(String group) {
        return this.groups.values().contains(group);
    }
}
