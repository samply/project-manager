package de.samply.query;

public enum QueryState {
    CREATED,
    TO_BE_SENT,
    TO_BE_SENT_AND_EXECUTED,
    SENDING,
    SENDING_AND_EXECUTING,
    ERROR,
    FINISHED
}
