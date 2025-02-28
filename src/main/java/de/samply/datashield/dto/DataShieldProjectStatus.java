package de.samply.datashield.dto;

public enum DataShieldProjectStatus {
    CREATED, // The project exists in the bridgehead opal
    WITH_DATA, // The project has already data in the bridgehead opal
    NOT_FOUND, // The project wasn't found in the bridghead opal
    INACTIVE, // The token manager is not enabled in project manager
    ERROR // There was an error while fetching the status
}
