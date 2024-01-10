package de.samply.frontend.dto;

import de.samply.notification.OperationType;

import java.time.Instant;

public record Notification(
        String email,
        Instant timestamp,
        String projectCode,
        String bridgehead,
        OperationType operationType,
        String details,
        String error
) {
}
