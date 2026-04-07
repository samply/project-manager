package de.samply.project.event;

import de.samply.db.model.Project;

@SuppressWarnings("unused")
public interface ProjectEventActions {

    String draft(String[] bridgeheads, String queryCode) throws ProjectEventActionsException;

    void create(Project project) throws ProjectEventActionsException;

    void accept(Project project) throws ProjectEventActionsException;

    void reject(Project project) throws ProjectEventActionsException;

    void archive(Project project) throws ProjectEventActionsException;

    void startDevelopStage(Project project) throws ProjectEventActionsException;

    void startPilotStage(Project project) throws ProjectEventActionsException;

    void startFinalStage(Project project) throws ProjectEventActionsException;

    void finish(Project project) throws ProjectEventActionsException;

}
