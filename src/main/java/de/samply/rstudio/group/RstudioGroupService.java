package de.samply.rstudio.group;

public interface RstudioGroupService {

    void addUserToRstudioGroup(String email) throws RstudioGroupServiceException;

    void removeUserFromRstudioGroup(String email) throws RstudioGroupServiceException;

}
