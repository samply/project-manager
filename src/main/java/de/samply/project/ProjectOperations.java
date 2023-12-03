package de.samply.project;

import de.samply.db.model.Project;

public interface ProjectOperations {

    Project draft(String projectName, String contactId);

    void create(String projectName);

    void accept(String projectName);

    void reject(String projectName);

    void archive(String projectName);

    void startDevelopStage(String projectName);

    void startPilotStage(String projectName);

    void startFinalStage(String projectName);

    void finish(String projectName);

}
