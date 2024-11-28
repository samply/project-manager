package de.samply.frontend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.samply.notification.OperationType;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Notification(
        Long id,
        String email,
        Instant timestamp,
        String projectCode,
        String bridgehead,
        String humanReadableBridgehead,
        OperationType operationType,
        String details,
        String error,
        HttpStatus httpStatus,
        Boolean read
) {
}
