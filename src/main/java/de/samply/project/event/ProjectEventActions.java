package de.samply.project.event;

import de.samply.project.ProjectType;

public interface ProjectEventActions {

    String draft(String[] bridgeheads, String queryCode, ProjectType projectType) throws ProjectEventActionsException;

    void create(String projectCode) throws ProjectEventActionsException;

    void accept(String projectCode) throws ProjectEventActionsException;

    void reject(String projectCode) throws ProjectEventActionsException;

    void archive(String projectCode) throws ProjectEventActionsException;

    void startDevelopStage(String projectCode) throws ProjectEventActionsException;

    void startPilotStage(String projectCode) throws ProjectEventActionsException;

    void startFinalStage(String projectCode) throws ProjectEventActionsException;

    void finish(String projectCode) throws ProjectEventActionsException;

}
