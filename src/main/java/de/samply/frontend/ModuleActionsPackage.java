package de.samply.frontend;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleActionsPackage {
    private String module;
    private List<Action> actions = new ArrayList<>();

    public void setModule(String module) {
        this.module = module;
    }

    public void addAction(Action action) {
        actions.add(action);
    }
}
