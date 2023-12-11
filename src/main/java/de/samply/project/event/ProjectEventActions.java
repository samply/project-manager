package de.samply.project.event;

public interface ProjectEventActions {

    void draft(String projectCode, String[] bridgeheads) throws ProjectEventActionsException;

    void create(String projectCode) throws ProjectEventActionsException;

    void accept(String projectCode) throws ProjectEventActionsException;

    void reject(String projectCode) throws ProjectEventActionsException;

    void archive(String projectCode) throws ProjectEventActionsException;

    void startDevelopStage(String projectCode) throws ProjectEventActionsException;

    void startPilotStage(String projectCode) throws ProjectEventActionsException;

    void startFinalStage(String projectCode) throws ProjectEventActionsException;

    void finish(String projectCode) throws ProjectEventActionsException;

}
