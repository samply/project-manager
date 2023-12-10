package de.samply.project.event;

public interface ProjectEventActions {

    void draft(String projectName, String[] bridgeheads) throws ProjectEventActionsException;

    void create(String projectName) throws ProjectEventActionsException;

    void accept(String projectName) throws ProjectEventActionsException;

    void reject(String projectName) throws ProjectEventActionsException;

    void archive(String projectName) throws ProjectEventActionsException;

    void startDevelopStage(String projectName) throws ProjectEventActionsException;

    void startPilotStage(String projectName) throws ProjectEventActionsException;

    void startFinalStage(String projectName) throws ProjectEventActionsException;

    void finish(String projectName) throws ProjectEventActionsException;

}
