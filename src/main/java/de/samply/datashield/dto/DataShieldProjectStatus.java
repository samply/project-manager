package de.samply.datashield.dto;


// Status comes from Token Manager
@SuppressWarnings("unused")
public enum DataShieldProjectStatus {
    CREATED, // The project exists in the bridgehead opal
    WITH_DATA, // The project already has data in the bridgehead opal
    NOT_FOUND, // The project wasn't found in the bridgehead opal
    INACTIVE, // The token manager is not enabled in the project manager
    ERROR // There was an error while fetching the status
}
