package de.samply.rstudio.group;

public interface RstudioGroupManager {

    void addUserToRstudioGroup(String email) throws RstudioGroupManagerException;

    void removeUserFromRstudioGroup(String email) throws RstudioGroupManagerException;

}
