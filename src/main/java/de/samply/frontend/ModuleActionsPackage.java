package de.samply.frontend;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ModuleActionsPackage {
    private String module;
    private Map<String, Action> actions = new HashMap<>();

    public void setModule(String module) {
        this.module = module;
    }

    public void addAction(String actionName, Action action) {
        actions.put(actionName, action);
    }

}
