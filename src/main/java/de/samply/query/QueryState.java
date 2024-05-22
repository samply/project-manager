package de.samply.query;

public enum QueryState {
    CREATED,
    TO_BE_SENT,
    TO_BE_SENT_AND_EXECUTED,
    SENDING,
    SENDING_AND_EXECUTING,
    EXPORT_RUNNING_1,
    EXPORT_RUNNING_2,
    ERROR,
    FINISHED
}
