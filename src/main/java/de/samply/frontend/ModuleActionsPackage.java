package de.samply.frontend;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleActionsPackage {
    private Module module;
    private List<Action> actions = new ArrayList<>();

    public void setModule(Module module) {
        this.module = module;
    }

    public void addAction(Action action) {
        actions.add(action);
    }
}
